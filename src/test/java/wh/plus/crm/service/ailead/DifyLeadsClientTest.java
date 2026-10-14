package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import wh.plus.crm.config.DifyConfig.DifyProperties;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Obsługa statusu odpowiedzi workflow Dify w {@link DifyLeadsClient}.
 *
 * <p>Kluczowy przypadek: {@code status=partial-succeeded} (część węzłów padła — np.
 * Zwiadowca/Kompas przez puste {@code LEADY_API_URL} — ale BZP/TED/Tavily dały raport)
 * NIE może wywalać całego przebiegu; parsujemy dostępne wyniki.
 */
class DifyLeadsClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    // RestTemplate nieużywany na ścieżce obsługi odpowiedzi — przekazujemy null świadomie.
    private final DifyLeadsClient client = new DifyLeadsClient(new DifyProperties(), null);

    private String load(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ailead/" + name)) {
            assertNotNull(in, "Brak zasobu testowego: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private JsonNode runResponse(String status, String raportResource) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode data = root.putObject("data");
        data.put("status", status);
        ObjectNode outputs = data.putObject("outputs");
        outputs.put("raport", load(raportResource));
        return root;
    }

    @Test
    void partialSucceededParsujeRaportZamiastWywalac() throws Exception {
        List<JsonNode> obiekty =
                client.handleRunResponse(runResponse("partial-succeeded", "dify-raport-wszystko.md"));

        assertEquals(11, obiekty.size(), "partial-succeeded z raportem powinno dać kandydatów");
    }

    @Test
    void succeededNadalParsuje() throws Exception {
        List<JsonNode> obiekty =
                client.handleRunResponse(runResponse("succeeded", "dify-raport-przetargi.md"));

        assertEquals(1, obiekty.size());
    }

    @Test
    void twardyBladStatusuWywala() {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode data = root.putObject("data");
        data.put("status", "failed");
        data.put("error", "boom");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.handleRunResponse(root));
        assertTrue(ex.getMessage().contains("failed"), "komunikat powinien zawierać status");
    }
}
