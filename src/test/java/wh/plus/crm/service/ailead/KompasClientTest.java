package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapowanie zleceń Kompas ({@code /v2/api/orders}) na kandydatów oraz wyciąganie
 * tokenu z odpowiedzi logowania ({@code /v1/auth/api/login}).
 */
class KompasClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    // RestTemplate nieużywany w mapOrders/extractToken — null świadomie.
    private final KompasClient client = new KompasClient(null, "info@w-h.pl", "x");

    private JsonNode load(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ailead/" + name)) {
            assertNotNull(in, "Brak zasobu: " + name);
            return mapper.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void mapujeZleceniaNaKandydatow() throws Exception {
        List<JsonNode> c = client.mapOrders(load("kompas-orders.json"));
        assertEquals(2, c.size());

        JsonNode a = c.get(0);
        assertEquals("Zabudowa terenu dawnej Elektrowni Szombierki - rewitalizacja", a.get("nazwa").asText());
        assertEquals("Bytom", a.get("miasto").asText());
        assertEquals("śląskie", a.get("wojewodztwo").asText());
        assertEquals("hotel", a.get("rodzaj").asText()); // subsector "hotele, pensjonaty"
        assertTrue(a.get("punkty").asInt() > 0);

        JsonNode s = a.get("sygnaly").get(0);
        assertEquals("Kompas", s.get("zrodlo").asText());
        assertTrue(s.get("url").asText().contains("kompasinwestycji.pl"));
        assertEquals("2026-11-14", s.get("termin").asText());
        assertEquals("Inwestor Hotelowy Sp. z o.o.", a.path("szczegoly").path("inwestor").asText());
    }

    @Test
    void wyciagaAccessTokenZLogowania() {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("success", true);
        resp.putObject("tokens").put("accessToken", "eyJabc.def.ghi").put("refreshToken", "r");

        assertEquals("eyJabc.def.ghi", KompasClient.extractToken(resp));
        assertNull(KompasClient.extractToken(null));
        assertNull(KompasClient.extractToken(mapper.createObjectNode()));
    }
}
