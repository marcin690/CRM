package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapowanie odpowiedzi publicznego API Zwiadowca.pl ({@code searchInvested.json})
 * na kandydatów w formacie zgodnym z {@link LeadCandidateProcessor}.
 */
class ZwiadowcaClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    // RestTemplate nieużywany w mapInvested — null świadomie.
    private final ZwiadowcaClient client = new ZwiadowcaClient(null, "289");

    private JsonNode load(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ailead/" + name)) {
            assertNotNull(in, "Brak zasobu: " + name);
            return mapper.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void mapujeZapytaniaNaKandydatow() throws Exception {
        List<JsonNode> c = client.mapInvested(load("zwiadowca-invested.json"));

        assertEquals(2, c.size(), "rekord z pustym tytułem ma być pominięty");

        JsonNode a = c.get(0);
        assertEquals("Zakup 10 łóżek hotelowych 160×200 cm z materacami do pensjonatu",
                a.get("nazwa").asText());
        assertEquals("zachodniopomorskie", a.get("wojewodztwo").asText()); // province=16
        assertEquals("hotel", a.get("rodzaj").asText());
        assertTrue(a.get("punkty").asInt() > 0, "punkty muszą być > 0 (sort na froncie)");
        assertFalse(a.get("przetarg").asBoolean(), "zapytanie to nie przetarg");

        JsonNode s = a.get("sygnaly").get(0);
        assertEquals("Zwiadowca", s.get("zrodlo").asText());
        assertEquals("https://zwiadowca.pl/inwestycja/4224744_zakup-lozek-hotelowych",
                s.get("url").asText());
        assertEquals("2026-10-15", s.get("data_publikacji").asText());
        assertEquals("2026-10-19", s.get("termin").asText());

        assertEquals("dolnośląskie", c.get(1).get("wojewodztwo").asText()); // province=1
    }

    @Test
    void pustaOdpowiedzDajePustaListe() {
        assertTrue(client.mapInvested(null).isEmpty());
        assertTrue(client.mapInvested(mapper.createObjectNode()).isEmpty());
    }
}
