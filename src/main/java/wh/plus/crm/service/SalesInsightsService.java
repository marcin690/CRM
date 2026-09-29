package wh.plus.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.dashboard.SalesAnalyticsDTO;
import wh.plus.crm.dto.dashboard.SalesInsightsDTO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generuje wnioski AI (Claude / Anthropic Messages API) na podstawie agregatów cockpitu.
 * Wynik cache'owany per okres (TTL 2h), aby nie palić tokenów przy każdym wejściu na stronę.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesInsightsService {

    private final SalesAnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key:}")
    private String apiKey;
    @Value("${anthropic.model:claude-opus-4-8}")
    private String model;
    @Value("${anthropic.max-tokens:4096}")
    private int maxTokens;
    @Value("${anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;
    @Value("${anthropic.workspace-id:}")
    private String workspaceId;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 2 * 60 * 60 * 1000L;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private record Cached(long ts, SalesInsightsDTO dto) {}

    public SalesInsightsDTO getInsights(LocalDate dateFrom, LocalDate dateTo, boolean refresh, Long teamId) {
        String key = dateFrom + "|" + dateTo + "|" + teamId;
        Cached c = cache.get(key);
        if (!refresh && c != null && (System.currentTimeMillis() - c.ts()) < TTL_MS) {
            return c.dto();
        }
        if (apiKey == null || apiKey.isBlank()) {
            return error("Brak klucza Anthropic. Ustaw ANTHROPIC_API_KEY (local.properties lub zmienna środowiskowa).");
        }
        SalesAnalyticsDTO analytics = analyticsService.getAnalytics(dateFrom, dateTo, null, null, teamId);
        SalesInsightsDTO dto = callClaude(analytics, dateFrom, dateTo);
        if (dto.getError() == null) {
            cache.put(key, new Cached(System.currentTimeMillis(), dto));
        }
        return dto;
    }

    private SalesInsightsDTO callClaude(SalesAnalyticsDTO analytics, LocalDate from, LocalDate to) {
        try {
            String system = """
                    Jesteś doświadczonym analitykiem sprzedaży w firmie WH-Plus (producent mebli i wyposażenia \
                    wnętrz hotelowych, B2B, wysokie kontrakty). Analizujesz CAŁY cykl sprzedaży: lead, oferta, umowa. \
                    Dane wejściowe zawierają obie perspektywy: LEADY (kpi, sources, reps, clients, industries, trend, \
                    heatmapy powodów odrzucenia leadów) oraz OFERTY (offers.statuses to rozkład statusów DRAFT/SENT/\
                    ACCEPTED/REJECTED/SIGNED z liczbą i wartością, offers.winRate to procent wygranych wśród rozstrzygniętych, \
                    offers.rejectionReasons to powody odrzucenia OFERT, offers.objectTypes i offers.investorTypes to segmentacja \
                    wartościowa), a także konwersje między etapami (cycle: lead do oferty, oferta do wysłania, wysłana do \
                    akceptacji, akceptacja do podpisania) i czasy procesu (timing). \
                    W analizie obowiązkowo połącz obie perspektywy: oceń nie tylko dopływ i jakość leadów, ale przede wszystkim \
                    skuteczność ofertowania (win-rate, struktura statusów, wartość utracona w odrzuconych ofertach, wąskie gardła \
                    między etapami, najcenniejsze segmenty obiektów i inwestorów). Wskaż, na którym etapie cyklu firma traci \
                    najwięcej i co z tego wynika. \
                    Piszesz WYŁĄCZNIE po polsku, rzeczowo, z konkretnymi liczbami z danych, bez marketingowego lania wody. \
                    NIE używaj myślnika ani pauzy (—); pisz zwykłymi, pełnymi zdaniami. \
                    Zwróć TYLKO surowy JSON (bez bloków ```), dokładnie w formacie: \
                    {"summary": "2-3 zdania oceny sytuacji", "wnioski": ["..."], "rekomendacje": ["..."], "ryzyka": ["..."]}. \
                    wnioski: 4-6 pozycji (w tym co najmniej dwie o ofertach lub konwersji między etapami), rekomendacje: 2-4, \
                    ryzyka: 1-3. Każda pozycja to jedno konkretne zdanie z liczbą, jeśli to możliwe. \
                    Nie dodawaj żadnego tekstu poza JSON-em.""";

            String analyticsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analytics);
            String userContent = "Dane pełnego cyklu sprzedaży (leady, oferty, umowy) za okres od " + from + " do " + to + ":\n" + analyticsJson;

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("system", system);
            ArrayNode messages = body.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", userContent);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/v1/messages"))
                    .timeout(Duration.ofSeconds(60))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (workspaceId != null && !workspaceId.isBlank()) {
                reqBuilder.header("anthropic-workspace-id", workspaceId);
            }
            HttpRequest req = reqBuilder.build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("Anthropic API zwróciło {}: {}", res.statusCode(), snippet(res.body()));
                return error("Anthropic API zwróciło status " + res.statusCode() + ".");
            }

            JsonNode root = objectMapper.readTree(res.body());
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                return error("Pusta odpowiedź modelu.");
            }
            // Model może zwrócić blok 'thinking' przed 'text' — sklejamy wszystkie bloki tekstowe.
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    sb.append(block.path("text").asText(""));
                }
            }
            return parsePayload(sb.toString());

        } catch (Exception e) {
            log.error("Błąd generowania wniosków AI cockpitu", e);
            return error("Błąd połączenia z Anthropic: " + e.getMessage());
        }
    }

    private SalesInsightsDTO parsePayload(String text) {
        try {
            String cleaned = stripFences(text);
            JsonNode node = objectMapper.readTree(cleaned);
            SalesInsightsDTO dto = new SalesInsightsDTO();
            dto.setSummary(node.path("summary").asText(""));
            dto.setWnioski(toList(node.path("wnioski")));
            dto.setRekomendacje(toList(node.path("rekomendacje")));
            dto.setRyzyka(toList(node.path("ryzyka")));
            dto.setModel(model);
            dto.setGeneratedAt(LocalDateTime.now().format(TS));
            return dto;
        } catch (Exception e) {
            log.warn("Nie udało się sparsować JSON od modelu: {}", snippet(text));
            return error("Model zwrócił odpowiedź w nieoczekiwanym formacie.");
        }
    }

    private List<String> toList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> out.add(n.asText()));
        }
        return out;
    }

    private String stripFences(String text) {
        String t = text == null ? "" : text.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) t = t.substring(start, end + 1);
        return t.trim();
    }

    private String snippet(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    private SalesInsightsDTO error(String msg) {
        SalesInsightsDTO dto = new SalesInsightsDTO();
        dto.setError(msg);
        dto.setModel(model);
        return dto;
    }
}
