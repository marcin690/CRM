package wh.plus.crm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.repository.LeadRepository;
import wh.plus.crm.repository.LeadSourceRepository;
import wh.plus.crm.service.ClientGlobalIdService;
import wh.plus.crm.service.MinioUploadService;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publiczny endpoint do przyjmowania zgłoszeń z landing-page'y (np. /wycena).
 * NIE wymaga uwierzytelnienia — patrz SecurityConfig.AUTH_WHITELIST.
 *
 * Tworzy {@link Lead} z minimalnym zestawem pól, dołącza linki do uploadowanych
 * plików (MinIO) w polu description. Nie modyfikuje istniejących encji ani schemy.
 *
 * Ochrona przed spamem:
 *  - honeypot (pole 'website' powinno być puste)
 *  - in-memory rate limit per IP (10 zgłoszeń / 10 minut)
 *  - limit rozmiaru i liczby plików
 */
@RestController
@RequestMapping("/api/form-submissions")
@RequiredArgsConstructor
@Slf4j
public class PublicLeadController {

    private final LeadRepository leadRepository;
    private final LeadSourceRepository leadSourceRepository;
    private final ClientGlobalIdService clientGlobalIdService;
    private final MinioUploadService minioUploadService;

    @Value("${recaptcha.secret-key:}")
    private String recaptchaSecretKey;

    @Value("${recaptcha.min-score:0.5}")
    private double recaptchaMinScore;

    private static final int MAX_FILES = 8;
    private static final long MAX_FILE_BYTES = 15L * 1024 * 1024; // 15 MB / plik
    private static final int RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 min
    private static final int RATE_LIMIT_MAX = 10;
    private static final String FALLBACK_SOURCE = "WEBSITE_CONTACT";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Prosty in-memory rate-limit. Wystarczy dla LP; przy skali można podmienić na Bucket4j/Redis.
    private final Map<String, Deque<Long>> rateBuckets = new ConcurrentHashMap<>();

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @Transactional
    public ResponseEntity<Map<String, Object>> submit(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "roomsRange", required = false) String roomsRange,
            @RequestParam(value = "executionRange", required = false) String executionRange,
            @RequestParam(value = "scope", required = false) String scope,           // CSV: "Pokoje,Łóżka"
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "source", required = false) String source,         // np. "WEBSITE_CONTACT" lub "GOOGLE_ADS"
            @RequestParam(value = "sourceMeta", required = false) String sourceMeta, // utm_*, gclid, referrer
            @RequestParam(value = "recaptchaToken", required = false) String recaptchaToken,
            @RequestParam(value = "returningClient", required = false) Boolean returningClient,
            @RequestParam(value = "sourceUrl", required = false) String sourceUrl,   // pełny URL LP / strony z której przyszedł lead
            @RequestParam(value = "website", required = false) String honeypot,      // honeypot
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            HttpServletRequest request
    ) {
        // Honeypot — bot wypełni ukryte pole 'website'. Odpowiadamy 200 OK,
        // żeby nie zdradzać że to spam-trap.
        if (honeypot != null && !honeypot.isBlank()) {
            log.info("Honeypot tripped from ip={}", clientIp(request));
            return ResponseEntity.ok(Map.of("success", true, "leadId", -1L));
        }

        // Rate-limit
        String ip = clientIp(request);
        if (!allowByRate(ip)) {
            log.warn("Rate limit exceeded ip={}", ip);
            return ResponseEntity.status(429).body(Map.of(
                    "success", false,
                    "error", "Za dużo zgłoszeń. Spróbuj ponownie za chwilę."
            ));
        }

        // Walidacja minimum
        if (isBlank(name) || isBlank(email) || isBlank(phone)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Imię, e-mail i telefon są wymagane."
            ));
        }

        // reCAPTCHA — aktywna tylko gdy ustawiony RECAPTCHA_SECRET_KEY.
        // Gdy puste → cicho pomijamy (faza pre-launch). Honeypot + rate-limit nas pokrywają.
        if (!isBlank(recaptchaSecretKey)) {
            RecaptchaResult v = verifyRecaptcha(recaptchaToken, ip);
            if (!v.ok) {
                log.warn("reCAPTCHA failed ip={} reason={} score={}", ip, v.reason, v.score);
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "Weryfikacja antyspamowa nie powiodła się. Spróbuj ponownie lub zadzwoń."
                ));
            }
        }

        // Upload plików (jeśli są)
        List<String> attachmentUrls = new ArrayList<>();
        if (files != null) {
            int count = 0;
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                if (++count > MAX_FILES) {
                    log.warn("Too many files in submission ip={}", ip);
                    break;
                }
                if (f.getSize() > MAX_FILE_BYTES) {
                    log.warn("File too large skipped: {} ({} bytes)", f.getOriginalFilename(), f.getSize());
                    continue;
                }
                try {
                    String url = minioUploadService.uploadFile(f);
                    attachmentUrls.add(url);
                } catch (Exception e) {
                    log.error("Upload failed for file {}: {}", f.getOriginalFilename(), e.getMessage());
                }
            }
        }

        // Mapowanie na encję Lead
        Lead lead = new Lead();
        lead.setClientFullName(trim(name));
        lead.setClientBusinessName(trim(company));
        lead.setClientEmail(trim(email));
        lead.setClientPhone(parsePhoneToLong(phone));
        lead.setName(buildLeadName(name, company));
        lead.setRoomsQuantity(parseRoomsQuantity(roomsRange));
        lead.setExecutionDate(parseExecutionDate(executionRange));
        lead.setDescription(buildDescription(scope, message, executionRange, sourceMeta, sourceUrl, attachmentUrls));
        lead.setClientGlobalId(clientGlobalIdService.generateClientGlobalId());
        lead.setLeadSource(resolveLeadSource(source));
        lead.setReturningClient(returningClient != null ? returningClient : Boolean.FALSE);
        lead.setSourceUrl(truncate(trim(sourceUrl), 500));

        Lead saved = leadRepository.save(lead);
        log.info("Public lead saved id={} source='{}' attachments={} ip={}",
                saved.getId(), lead.getLeadSource() != null ? lead.getLeadSource().getName() : "-",
                attachmentUrls.size(), ip);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "leadId", saved.getId(),
                "attachments", attachmentUrls
        ));
    }

    // ===== helpers =====

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String buildLeadName(String name, String company) {
        if (!isBlank(company)) return company.trim();
        return isBlank(name) ? "Zgłoszenie z LP" : name.trim();
    }

    /** "+48 600 000 000" -> 48600000000L. Zwraca null jeśli się nie da. */
    static Long parsePhoneToLong(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try { return Long.parseLong(digits); } catch (NumberFormatException e) { return null; }
    }

    /** "Do 20" / "20 – 50" / "Powyżej 200" -> górna granica jako Long. */
    static Long parseRoomsQuantity(String range) {
        if (range == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(range);
        Long last = null;
        while (m.find()) {
            try { last = Long.parseLong(m.group()); } catch (NumberFormatException ignored) {}
        }
        return last;
    }

    /** "Do 3 miesięcy" / "3 – 6 miesięcy" / "Powyżej 12 miesięcy" -> data = teraz + górna granica miesięcy. */
    static LocalDateTime parseExecutionDate(String range) {
        if (range == null) return null;
        Long months = parseRoomsQuantity(range); // ten sam mechanizm — bierze ostatnią liczbę
        if (months == null) return null;
        return LocalDateTime.now().plusMonths(months);
    }

    /**
     * Buduje czytelny opis leada — plain text z liniami i sekcjami.
     * W UI rendered przez {@code white-space: pre-wrap} (LeadDetails.js), więc
     * linie i wcięcia są zachowane bez wstrzykiwania HTML-a (zero ryzyka XSS).
     *
     * Format:
     *   ── Lead utworzony automatycznie z formularza ──
     *
     *   ▸ Zakres wyposażenia
     *       Pokoje, Łóżka, Restauracja
     *
     *   ▸ Planowany termin
     *       Powyżej 12 miesięcy
     *
     *   ▸ Wiadomość od klienta
     *       Treść wpisana w pole opis…
     *
     *   ▸ Strona źródłowa
     *       https://wyposazenie-hotelowe.pl/wycena?gclid=…
     *
     *   ▸ Kampania / UTM
     *       utm_source=google | utm_medium=cpc | gclid=…
     *
     *   ▸ Załączniki (2)
     *       • https://media.w-h.pl/crm-uploads/abc.pdf
     *       • https://…
     */
    private static String buildDescription(String scope, String message, String executionRange,
                                           String sourceMeta, String sourceUrl, List<String> attachments) {
        StringBuilder sb = new StringBuilder();
        sb.append("── Lead utworzony automatycznie z formularza ──\n");

        appendSection(sb, "Zakres wyposażenia", scope);
        appendSection(sb, "Planowany termin",   executionRange);
        appendSection(sb, "Wiadomość od klienta", message);
        appendSection(sb, "Strona źródłowa",    sourceUrl);
        appendSection(sb, "Kampania / UTM",     sourceMeta);

        if (attachments != null && !attachments.isEmpty()) {
            sb.append("\n▸ Załączniki (").append(attachments.size()).append(")\n");
            for (String url : attachments) sb.append("    • ").append(url).append('\n');
        }
        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) return;
        sb.append("\n▸ ").append(label).append('\n');
        // Wcięcie każdej linii zawartości — czytelność przy white-space: pre-wrap
        for (String line : value.trim().split("\\r?\\n")) {
            sb.append("    ").append(line).append('\n');
        }
    }

    /**
     * Rozpoznaje LeadSource po kodzie (np. "GOOGLE_ADS", "WEBSITE_CONTACT") wyłącznie
     * spośród rekordów już istniejących w bazie. NIE tworzy nowych — żeby zewnętrzny
     * formularz nie mógł zaśmiecać słownika przypadkowymi wartościami.
     *
     * Zalecenie: prawidłowe kanały marketingowe (GOOGLE_ADS, FACEBOOK_ADS, …) zakłada
     * Flyway-migracja V9 / admin ręcznie. Brak dopasowania → fallback do WEBSITE_CONTACT.
     */
    private LeadSource resolveLeadSource(String source) {
        String code = (source != null) ? source.trim().toUpperCase(Locale.ROOT) : "";
        if (!code.isEmpty()) {
            Optional<LeadSource> found = leadSourceRepository.findByName(code);
            if (found.isPresent()) return found.get();
            log.info("Unknown lead source code '{}' — falling back to {}", code, FALLBACK_SOURCE);
        }
        // Fallback: WEBSITE_CONTACT — istnieje w base data (id=1, vide tabela lead_source).
        return leadSourceRepository.findByName(FALLBACK_SOURCE).orElse(null);
    }

    // ===== reCAPTCHA v3 =====
    private record RecaptchaResult(boolean ok, double score, String reason) {}

    private RecaptchaResult verifyRecaptcha(String token, String ip) {
        if (isBlank(token)) return new RecaptchaResult(false, 0.0, "missing-token");
        try {
            String body = "secret=" + URLEncoder.encode(recaptchaSecretKey, StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + (isBlank(ip) ? "" : "&remoteip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.google.com/recaptcha/api/siteverify"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(resp.body());
            boolean success = json.path("success").asBoolean(false);
            double score = json.path("score").asDouble(0.0);
            String action = json.path("action").asText("");
            if (!success) {
                return new RecaptchaResult(false, score, "google-rejected:" + json.path("error-codes"));
            }
            if (!"lead_form".equals(action)) {
                return new RecaptchaResult(false, score, "wrong-action:" + action);
            }
            if (score < recaptchaMinScore) {
                return new RecaptchaResult(false, score, "low-score");
            }
            return new RecaptchaResult(true, score, "ok");
        } catch (Exception e) {
            log.warn("reCAPTCHA verification error: {}", e.getMessage());
            // Fail-open w razie błędu sieci — wolimy złapać 1 spam niż stracić leada przy padzie Google.
            // Honeypot + rate-limit i tak nas pokrywają.
            return new RecaptchaResult(true, 0.0, "verifier-error-fail-open");
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return req.getRemoteAddr() == null ? "unknown" : req.getRemoteAddr();
    }

    private boolean allowByRate(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> times = rateBuckets.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > RATE_LIMIT_WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() >= RATE_LIMIT_MAX) return false;
            times.addLast(now);
        }
        // Sprzątanie pamięci — okazjonalnie
        if (rateBuckets.size() > 10_000) rateBuckets.entrySet().removeIf(e -> e.getValue().isEmpty());
        return true;
    }
}
