package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import wh.plus.crm.config.DifyConfig.DifyProperties;
import wh.plus.crm.model.lead.LeadCandidate;
import wh.plus.crm.model.lead.LeadCandidateRun;
import wh.plus.crm.model.lead.LeadCandidateRunStatus;
import wh.plus.crm.model.lead.LeadCandidateStatus;
import wh.plus.crm.repository.LeadCandidateRepository;
import wh.plus.crm.repository.LeadCandidateRunRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Wykonuje przebieg agenta w tle: woła Dify (długie, poza transakcją), parsuje
 * kandydatów i zapisuje z deduplikacją. Aktualizuje status przebiegu.
 *
 * <p>Osobny bean od {@link LeadCandidateService}, żeby proxy {@code @Async}
 * działało (unikamy self-invocation) i nie trzymać transakcji DB podczas
 * wielominutowego wywołania Dify.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeadCandidateProcessor {

    /** Skróty pomijane przy budowie klucza deduplikacji (jak w workflow). */
    private static final Set<String> SKROTY = Set.of(
            "hotel", "hotelu", "hotelowy", "spa", "resort", "wellness", "medical",
            "sp", "z", "o", "oo", "s", "a", "sa", "sk", "gmina", "miasto", "powiat");

    private final LeadCandidateRunRepository runRepository;
    private final LeadCandidateRepository candidateRepository;
    private final DifyLeadsClient difyLeadsClient;
    private final DifyProperties difyProperties;
    private final ZwiadowcaClient zwiadowcaClient;
    private final KompasClient kompasClient;

    @Async("valuationExecutor")
    public void process(Long runId) {
        LeadCandidateRun run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            log.warn("Przebieg leadów {} nie istnieje — pomijam", runId);
            return;
        }
        try {
            String tryb = run.getTryb();
            List<JsonNode> leads = new ArrayList<>();

            // Dify = BZP/TED/Tavily (newsy). Pomijamy tylko dla trybów wyłącznie Zwiadowca/Kompas.
            if (wantsDify(tryb)) {
                try {
                    String user = difyProperties.getUserPrefix() + ":system";
                    leads.addAll(difyLeadsClient.fetchLeads(
                            tryb, run.getDniNewsy(), run.getDniPrzetargi(), user));
                } catch (Exception e) {
                    log.warn("Dify nieudane (kontynuuję z pozostałymi źródłami): {}", e.getMessage());
                }
            }
            // Zwiadowca i Kompas — bezpośrednio z ich API (każde łapie własne błędy).
            if (wantsZwiadowca(tryb)) leads.addAll(zwiadowcaClient.fetchCandidates());
            if (wantsKompas(tryb)) leads.addAll(kompasClient.fetchCandidates());

            int found = 0;
            for (JsonNode n : leads) {
                if (upsert(n)) found++;
            }

            run.setFound(found);
            run.setStatus(LeadCandidateRunStatus.DONE);
            run.setFinishedAt(LocalDateTime.now());
            run.setMessage("Przetworzono " + leads.size() + ", nowych kandydatów: " + found);
            runRepository.save(run);
            log.info("Przebieg leadów {} zakończony: {} nowych z {}", runId, found, leads.size());
        } catch (Exception e) {
            log.error("Przebieg leadów {} nieudany: {}", runId, e.getMessage(), e);
            run.setStatus(LeadCandidateRunStatus.FAILED);
            run.setFinishedAt(LocalDateTime.now());
            run.setMessage(trim(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), 1900));
            runRepository.save(run);
        }
    }

    // --- wybór źródeł wg trybu (wartości z UI: wszystko / przetargi i zapytania /
    //     tylko newsy / tylko zwiadowca / tylko kompas) ---

    private static boolean has(String tryb, String... keys) {
        String t = tryb == null ? "" : tryb.toLowerCase();
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }

    static boolean wantsZwiadowca(String tryb) {
        return has(tryb, "wszystk", "zwiadowc", "przetarg", "zapytan");
    }

    static boolean wantsKompas(String tryb) {
        return has(tryb, "wszystk", "kompas", "przetarg", "zapytan", "zlecen");
    }

    static boolean wantsDify(String tryb) {
        // Dify dostarcza BZP/TED/Tavily; pomijamy je tylko dla trybów wyłącznie Zwiadowca/Kompas.
        return !(has(tryb, "tylko") && has(tryb, "zwiadowc", "kompas"));
    }

    /** @return true jeśli dodano NOWEGO kandydata. */
    private boolean upsert(JsonNode n) {
        String nazwa = n.path("nazwa").asText("").trim();
        if (nazwa.isEmpty()) return false;
        String miasto = n.path("miasto").asText("").trim();
        String key = dedupKey(nazwa, miasto);

        LeadCandidate c = candidateRepository.findByDedupKey(key).orElse(null);
        boolean isNew = (c == null);
        if (!isNew && c.getStatus() != LeadCandidateStatus.NEW) {
            return false; // zaakceptowany/odrzucony — nie wskrzeszaj
        }
        if (isNew) {
            c = new LeadCandidate();
            c.setDedupKey(key);
            c.setStatus(LeadCandidateStatus.NEW);
            c.setGeneratedAt(LocalDateTime.now());
        }
        fill(c, n);
        candidateRepository.save(c);
        return isNew;
    }

    private void fill(LeadCandidate c, JsonNode n) {
        c.setNazwa(n.path("nazwa").asText("").trim());
        c.setMiasto(n.path("miasto").asText("").trim());
        c.setWojewodztwo(n.path("wojewodztwo").asText("").trim());
        c.setRodzaj(n.path("rodzaj").asText("").trim());
        c.setPunkty(n.path("punkty").asInt(0));
        c.setPrzetarg(n.path("przetarg").asBoolean(false));

        JsonNode sygnaly = n.path("sygnaly");
        c.setSygnalyJson(sygnaly.isArray() ? sygnaly.toString() : "[]");
        JsonNode szczegoly = n.path("szczegoly");
        c.setSzczegolyJson(szczegoly.isObject() ? szczegoly.toString() : "{}");

        String url = sygnaly.isArray() && sygnaly.size() > 0
                ? sygnaly.get(0).path("url").asText("") : "";
        c.setZrodloUrl(url.isBlank() ? null : trim(url, 1000));
    }

    // --- deduplikacja (odpowiednik funkcji klucz()/norm() w workflow) ---

    static String dedupKey(String nazwa, String miasto) {
        String full = norm(nazwa);
        StringBuilder krotka = new StringBuilder();
        for (String w : full.split(" ")) {
            if (!w.isEmpty() && !SKROTY.contains(w)) {
                if (krotka.length() > 0) krotka.append(' ');
                krotka.append(w);
            }
        }
        String base = krotka.length() > 0 ? krotka.toString() : full;
        String key = base + "|" + norm(miasto);
        return trim(key, 400);
    }

    static String norm(String t) {
        if (t == null) return "";
        String s = t.toLowerCase();
        String from = "ąćęłńóśźż";
        String to = "acelnoszz";
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            int i = from.indexOf(ch);
            sb.append(i >= 0 ? to.charAt(i) : ch);
        }
        return sb.toString().replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
