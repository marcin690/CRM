package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Pobiera zapytania ofertowe ze <b>Zwiadowca.pl</b> (publiczne API {@code front-api},
 * bez logowania) i mapuje je na kandydatów w formacie zgodnym z
 * {@link LeadCandidateProcessor} (nazwa, miasto, wojewodztwo, rodzaj, punkty,
 * przetarg, sygnaly[], szczegoly{}).
 */
@Service
@Slf4j
public class ZwiadowcaClient {

    private static final String URL =
            "https://zwiadowca.pl/front-api/invested/searchInvested.json"
                    + "?direction=DESC&page=1&sort=Invested.publish_date";

    /** Kody województw w Zwiadowcy = indeks alfabetyczny 1..16 (potwierdzone na danych). */
    private static final String[] WOJ = {
            "", "dolnośląskie", "kujawsko-pomorskie", "lubelskie", "lubuskie",
            "łódzkie", "małopolskie", "mazowieckie", "opolskie", "podkarpackie",
            "podlaskie", "pomorskie", "śląskie", "świętokrzyskie",
            "warmińsko-mazurskie", "wielkopolskie", "zachodniopomorskie"
    };

    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Integer> trades;

    public ZwiadowcaClient(@Qualifier("difyRestTemplate") RestTemplate rest,
                           @Value("${market.zwiadowca.trades:289}") String tradesCsv) {
        this.rest = rest;
        this.trades = parseTrades(tradesCsv);
    }

    private static List<Integer> parseTrades(String csv) {
        List<Integer> t = new ArrayList<>();
        if (csv != null) {
            for (String s : csv.split(",")) {
                s = s.trim();
                if (!s.isEmpty()) t.add(Integer.valueOf(s));
            }
        }
        return t;
    }

    /** Woła API i zwraca kandydatów; przy błędzie loguje i zwraca pustą listę (nie wywala przebiegu). */
    public List<JsonNode> fetchCandidates() {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("page", 1);
            body.put("active", true);
            body.put("ended", false);
            ArrayNode tr = body.putArray("trades");
            trades.forEach(tr::add);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.set("Referer", "https://zwiadowca.pl/serwis");

            ResponseEntity<JsonNode> resp = rest.postForEntity(
                    URL, new HttpEntity<>(body, headers), JsonNode.class);

            List<JsonNode> out = mapInvested(resp.getBody());
            log.info("Zwiadowca: {} zapytań (trades={})", out.size(), trades);
            return out;
        } catch (Exception e) {
            log.warn("Zwiadowca niedostępny: {}", e.getMessage());
            return List.of();
        }
    }

    /** Mapuje odpowiedź {@code {"invested":[...]}} na kandydatów. Czyste — testowalne. */
    List<JsonNode> mapInvested(JsonNode root) {
        List<JsonNode> out = new ArrayList<>();
        if (root == null) return out;
        JsonNode invested = root.path("invested");
        if (!invested.isArray()) return out;
        for (JsonNode it : invested) {
            String title = it.path("title").asText("").trim();
            if (title.isEmpty()) continue; // bez tytułu nie ma kandydata
            out.add(toCandidate(it, title));
        }
        return out;
    }

    private JsonNode toCandidate(JsonNode it, String title) {
        ObjectNode c = mapper.createObjectNode();
        c.put("nazwa", title);
        c.put("miasto", "");
        int prov = it.path("province").asInt(0);
        c.put("wojewodztwo", (prov >= 1 && prov < WOJ.length) ? WOJ[prov] : "");
        c.put("rodzaj", rodzaj(title));
        c.put("punkty", score(title));
        c.put("przetarg", false);

        String url = "https://zwiadowca.pl" + it.path("url").asText("");
        String opis = it.path("description").asText("").trim();
        if (opis.length() > 500) opis = opis.substring(0, 500);

        ObjectNode syg = mapper.createObjectNode();
        syg.put("etykieta", "Zapytanie ofertowe");
        syg.put("data_publikacji", date(it.path("publish_date").asText("")));
        String termin = date(it.path("oferts_time").asText(""));
        if (!termin.isEmpty()) syg.put("termin", termin);
        syg.put("url", url);
        syg.put("zrodlo", "Zwiadowca");
        syg.put("opis", opis);
        ArrayNode sygnaly = c.putArray("sygnaly");
        sygnaly.add(syg);

        ObjectNode szcz = c.putObject("szczegoly");
        if (it.path("points_cost").isNumber()) szcz.put("points_cost", it.path("points_cost").asInt());
        return c;
    }

    private static String date(String iso) {
        return (iso == null || iso.length() < 10) ? "" : iso.substring(0, 10);
    }

    private static String rodzaj(String title) {
        String t = title.toLowerCase();
        if (t.contains("hotel") || t.contains("pensjonat") || t.contains("apartament")
                || t.contains("resort") || t.contains("sanatorium") || t.contains("ośrod")) {
            return "hotel";
        }
        return "zapytanie ofertowe";
    }

    private static int score(String title) {
        String t = title.toLowerCase();
        int s = 40;
        for (String kw : new String[]{"hotel", "pensjonat", "apartament", "łóżk", "meble", "wyposażeni"}) {
            if (t.contains(kw)) s += 10;
        }
        return Math.min(s, 90);
    }
}
