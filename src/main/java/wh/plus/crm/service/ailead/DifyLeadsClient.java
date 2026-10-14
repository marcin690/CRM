package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import wh.plus.crm.config.DifyConfig.DifyProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Klient aplikacji Dify typu <b>workflow</b> („WH PLUS Leady"): wywołuje
 * {@code POST /workflows/run} (blocking) i czyta wyjście {@code leady_json}
 * — ustrukturyzowaną listę kandydatów.
 *
 * <p>Odrębny od {@link wh.plus.crm.service.aivaluation.DifyClient}, który obsługuje
 * aplikacje typu chatflow ({@code /chat-messages}).
 */
@Service
@Slf4j
public class DifyLeadsClient {

    /** Kod agenta → klucz w {@code dify.agent-keys.leady-rynek}. */
    public static final String AGENT_CODE = "leady-rynek";

    private final DifyProperties props;
    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();

    public DifyLeadsClient(DifyProperties props, @Qualifier("difyRestTemplate") RestTemplate rest) {
        this.props = props;
        this.rest = rest;
    }

    private String resolveKey() {
        String key = props.getAgentKeys().get(AGENT_CODE);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Brak klucza API Dify dla agenta leadów "
                    + "(ustaw dify.agent-keys." + AGENT_CODE + " / env DIFY_KEY_LEADY_RYNEK)");
        }
        return key;
    }

    /**
     * Uruchamia workflow i zwraca listę surowych obiektów-kandydatów z pola
     * {@code outputs.leady_json}. Gdy workflow nie wystawia jeszcze tego pola,
     * loguje ostrzeżenie i zwraca pustą listę (nie parsuje Markdown).
     */
    public List<JsonNode> fetchLeads(String tryb, int dniNewsy, int dniPrzetargi, String user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resolveKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("tryb", tryb);
        inputs.put("dni_newsy", dniNewsy);
        inputs.put("dni_przetargi", dniPrzetargi);
        // Workflow v5 ma też okna dla Zwiadowcy i Kompasu — nieeksponowane w UI, dajemy sensowny default.
        inputs.put("dni_zwiadowca", dniNewsy);
        inputs.put("dni_kompas", dniNewsy);

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", user);

        ResponseEntity<JsonNode> resp = rest.postForEntity(
                props.getBaseUrl() + "/workflows/run",
                new HttpEntity<>(body, headers),
                JsonNode.class);

        return handleRunResponse(resp.getBody());
    }

    /**
     * Przetwarza odpowiedź {@code /workflows/run}: sprawdza status, po czym czyta
     * {@code outputs.leady_json} (preferowane) lub parsuje Markdown {@code outputs.raport}.
     *
     * <p>Status {@code partial-succeeded} (część węzłów padła, ale workflow dobiegł końca
     * i wystawił raport) traktujemy jak sukces — parsujemy to, co dostępne. Twarde statusy
     * ({@code failed}, {@code stopped}) wywołują wyjątek.
     */
    List<JsonNode> handleRunResponse(JsonNode root) {
        if (root == null) {
            throw new IllegalStateException("Pusta odpowiedź z Dify (workflows/run)");
        }

        JsonNode data = root.path("data");
        String status = data.path("status").asText("");
        // 'partial-succeeded' = część węzłów padła (np. Zwiadowca/Kompas), ale workflow
        // dobiegł końca i wystawił raport — parsujemy to, co jest, zamiast odrzucać cały przebieg.
        if (!status.isEmpty() && !"succeeded".equals(status) && !"partial-succeeded".equals(status)) {
            String err = data.path("error").asText("");
            throw new IllegalStateException("Workflow Dify nie powiódł się (status=" + status
                    + (err.isEmpty() ? "" : ", error=" + err) + ")");
        }

        JsonNode outputs = data.path("outputs");

        // Preferuj ustrukturyzowane 'leady_json', jeśli workflow je wystawia.
        JsonNode leadyJsonNode = outputs.path("leady_json");
        if (!leadyJsonNode.isMissingNode() && !leadyJsonNode.isNull()) {
            JsonNode array = parseArray(leadyJsonNode);
            if (array != null && array.isArray()) {
                List<JsonNode> result = new ArrayList<>();
                array.forEach(result::add);
                return result;
            }
        }

        // Fallback: parsuj Markdown z pola 'raport' (to oddaje wdrożony workflow v5).
        JsonNode raportNode = outputs.path("raport");
        if (raportNode.isMissingNode() || raportNode.isNull()) {
            log.warn("Dify workflow nie zwrócił ani 'leady_json', ani 'raport'. Dostępne wyjścia: {}",
                    fieldNames(outputs));
            return List.of();
        }
        List<JsonNode> parsed = RaportParser.parse(raportNode.asText(""), mapper);
        log.info("Sparsowano {} obiektów z raportu Dify", parsed.size());
        return parsed;
    }

    /** {@code leady_json} to zwykle String (json.dumps); dopuszczamy też gotową tablicę. */
    private JsonNode parseArray(JsonNode node) {
        try {
            if (node.isTextual()) {
                return mapper.readTree(node.asText());
            }
            return node;
        } catch (Exception e) {
            log.warn("Nie udało się sparsować 'leady_json': {}", e.getMessage());
            return null;
        }
    }

    private String fieldNames(JsonNode outputs) {
        if (outputs == null || !outputs.isObject()) return "(brak)";
        List<String> names = new ArrayList<>();
        outputs.fieldNames().forEachRemaining(names::add);
        return String.join(", ", names);
    }
}
