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
 * Pobiera zlecenia budowlane z <b>Kompas Inwestycji</b> przez jego REST API (JWT):
 * logowanie {@code POST /v1/auth/api/login} → {@code Authorization: Bearer} →
 * {@code GET /v2/api/orders}. Mapuje na kandydatów w formacie
 * {@link LeadCandidateProcessor}. Nieaktywny, gdy brak {@code market.kompas.password}.
 */
@Service
@Slf4j
public class KompasClient {

    private static final String BASE = "https://kompasinwestycji.pl";
    private static final String LOGIN = BASE + "/v1/auth/api/login";
    private static final String ORDERS = BASE + "/v2/api/orders"
            + "?status=RELEASED&sort=PUBLISHED_DESC&size=50&page=0&byIndustries=false";
    private static final String ORDER_LINK = BASE + "/v2/zlecenia/";

    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String email;
    private final String password;

    public KompasClient(@Qualifier("difyRestTemplate") RestTemplate rest,
                        @Value("${market.kompas.email:}") String email,
                        @Value("${market.kompas.password:}") String password) {
        this.rest = rest;
        this.email = email == null ? "" : email;
        this.password = password == null ? "" : password;
    }

    /** Loguje się, pobiera zlecenia i mapuje; przy błędzie loguje i zwraca pustą listę. */
    public List<JsonNode> fetchCandidates() {
        if (password.isBlank()) {
            log.info("Kompas pominięty — brak market.kompas.password (env KOMPAS_PASSWORD)");
            return List.of();
        }
        try {
            String token = login();
            if (token == null) return List.of();

            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token);
            h.setAccept(List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<JsonNode> resp = rest.exchange(
                    ORDERS, HttpMethod.GET, new HttpEntity<>(h), JsonNode.class);

            List<JsonNode> out = mapOrders(resp.getBody());
            log.info("Kompas: {} zleceń", out.size());
            return out;
        } catch (Exception e) {
            log.warn("Kompas niedostępny: {}", e.getMessage());
            return List.of();
        }
    }

    private String login() {
        ObjectNode body = mapper.createObjectNode();
        body.put("username", email);
        body.put("password", password);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> r = rest.postForEntity(LOGIN, new HttpEntity<>(body, h), JsonNode.class);
        String token = extractToken(r.getBody());
        if (token == null) log.warn("Kompas login nieudany lub brak tokenu w odpowiedzi");
        return token;
    }

    /** Wyciąga {@code tokens.accessToken} z odpowiedzi logowania. */
    static String extractToken(JsonNode loginResp) {
        if (loginResp == null) return null;
        String t = loginResp.path("tokens").path("accessToken").asText("");
        return t.isBlank() ? null : t;
    }

    /** Mapuje {@code {"content":[...]}} (zlecenia) na kandydatów. Czyste — testowalne. */
    List<JsonNode> mapOrders(JsonNode root) {
        List<JsonNode> out = new ArrayList<>();
        if (root == null) return out;
        JsonNode content = root.path("content");
        if (!content.isArray()) return out;
        for (JsonNode o : content) {
            JsonNode inv = o.path("investment");
            String nazwa = inv.path("name").asText("").trim();
            if (nazwa.isEmpty()) continue;
            out.add(toCandidate(o, inv, nazwa));
        }
        return out;
    }

    private JsonNode toCandidate(JsonNode o, JsonNode inv, String nazwa) {
        ObjectNode c = mapper.createObjectNode();
        c.put("nazwa", nazwa);
        c.put("miasto", inv.path("city").path("name").asText("").trim());
        c.put("wojewodztwo", inv.path("city").path("province").asText("").trim());

        String subsektor = inv.path("subsector").path("name").asText("").trim();
        c.put("rodzaj", rodzaj(subsektor));
        c.put("punkty", score(subsektor, o.path("industries")));
        c.put("przetarg", false);

        String termin = o.path("deadlines").path("offerDeadlineTo").asText("");
        String link = ORDER_LINK + o.path("id").asText("");

        StringBuilder opis = new StringBuilder();
        String inwestor = o.path("company").path("name").asText("").trim();
        if (!subsektor.isEmpty()) opis.append(subsektor);
        List<String> branze = new ArrayList<>();
        o.path("industries").forEach(i -> {
            String n = i.path("name").asText("").trim();
            if (!n.isEmpty()) branze.add(n);
        });
        if (!branze.isEmpty()) opis.append(opis.length() > 0 ? " · " : "").append(String.join(", ", branze));

        ObjectNode syg = mapper.createObjectNode();
        syg.put("etykieta", "Zlecenie (Kompas)");
        syg.put("data_publikacji", date(o.path("publishAt").asText("")));
        if (termin != null && !termin.isBlank() && !"None".equals(termin)) syg.put("termin", date(termin));
        syg.put("url", link);
        syg.put("zrodlo", "Kompas");
        syg.put("opis", opis.toString());
        ArrayNode sygnaly = c.putArray("sygnaly");
        sygnaly.add(syg);

        ObjectNode szcz = c.putObject("szczegoly");
        if (!inwestor.isEmpty()) szcz.put("inwestor", inwestor);
        if (!subsektor.isEmpty()) szcz.put("subsektor", subsektor);
        return c;
    }

    private static String date(String iso) {
        return (iso == null || iso.length() < 10) ? "" : iso.substring(0, 10);
    }

    private static String rodzaj(String subsektor) {
        String s = subsektor.toLowerCase();
        if (s.contains("hotel") || s.contains("pensjonat") || s.contains("sanatorium")
                || s.contains("apartament") || s.contains("turyst")) {
            return "hotel";
        }
        return subsektor.isEmpty() ? "zlecenie" : subsektor;
    }

    private static int score(String subsektor, JsonNode industries) {
        int s = 35;
        String sub = subsektor.toLowerCase();
        if (sub.contains("hotel") || sub.contains("pensjonat") || sub.contains("sanatorium")) s += 25;
        if (industries != null) {
            for (JsonNode i : industries) {
                String n = i.path("name").asText("").toLowerCase();
                if (n.contains("wyposażeni") || n.contains("meble") || n.contains("wnętrz")) s += 10;
            }
        }
        return Math.min(s, 90);
    }
}
