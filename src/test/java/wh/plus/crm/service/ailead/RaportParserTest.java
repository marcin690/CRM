package wh.plus.crm.service.ailead;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test parsera raportu Dify na PRAWDZIWYCH odpowiedziach workflow „WH PLUS Leady"
 * (próbki w src/test/resources/ailead). Chroni przed rozjechaniem się formatu.
 */
class RaportParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private String load(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ailead/" + name)) {
            assertNotNull(in, "Brak zasobu testowego: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsujePrzetargZTed() throws Exception {
        List<JsonNode> obiekty = RaportParser.parse(load("dify-raport-przetargi.md"), mapper);

        assertEquals(1, obiekty.size(), "Oczekiwano 1 obiektu w próbce przetargi");
        JsonNode o = obiekty.get(0);
        assertEquals("Uzdrowisko Goczałkowice-Zdrój", o.get("nazwa").asText());
        assertEquals(50, o.get("punkty").asInt());
        assertTrue(o.get("przetarg").asBoolean());
        assertEquals("zamawiający publiczny", o.get("rodzaj").asText());
        assertEquals("633491-2026", o.path("szczegoly").path("numer").asText());

        assertEquals(1, o.get("sygnaly").size());
        JsonNode s = o.get("sygnaly").get(0);
        assertEquals("Przetarg na meble", s.get("etykieta").asText());
        assertEquals("2026-09-15", s.get("data_publikacji").asText());
        assertTrue(s.get("url").asText().contains("ted.europa.eu"));
        assertEquals("TED 633491-2026", s.get("zrodlo").asText());
        assertFalse(s.get("opis").asText().isBlank());
    }

    @Test
    void parsujePelnyPrzebiegZeWszystkichZrodel() throws Exception {
        List<JsonNode> obiekty = RaportParser.parse(load("dify-raport-wszystko.md"), mapper);

        assertEquals(11, obiekty.size(), "Oczekiwano 11 obiektów w próbce wszystko");

        // Każdy obiekt ma sensowne minimum: nazwę, punkty i co najmniej jeden sygnał z URL.
        for (JsonNode o : obiekty) {
            assertFalse(o.get("nazwa").asText().isBlank(), "pusta nazwa obiektu");
            assertTrue(o.get("punkty").asInt() > 0, "zerowe punkty");
            assertTrue(o.get("sygnaly").size() >= 1, "obiekt bez sygnałów: " + o.get("nazwa").asText());
            for (JsonNode s : o.get("sygnaly")) {
                assertFalse(s.path("url").asText().isBlank(), "sygnał bez URL");
                assertFalse(s.path("etykieta").asText().isBlank(), "sygnał bez etykiety");
            }
        }

        // Obiekt z newsów: lokalizacja w nawiasie + szczegóły z detali.
        JsonNode marriott = find(obiekty, "AC by Marriott Jachranka");
        assertEquals("Jachranka", marriott.get("miasto").asText());
        assertEquals("mazowieckie", marriott.get("wojewodztwo").asText());
        assertEquals("hotel", marriott.get("rodzaj").asText());
        assertFalse(marriott.get("przetarg").asBoolean(), "news to nie 'przetarg'");
        assertFalse(marriott.path("szczegoly").path("inwestor").asText().isBlank());
        assertFalse(marriott.path("szczegoly").path("operator").asText().isBlank());

        // Obiekt z sekcji przetargów ma flagę przetarg=true.
        JsonNode przetarg = find(obiekty, "Uzdrowisko Goczałkowice-Zdrój");
        assertTrue(przetarg.get("przetarg").asBoolean());
    }

    private JsonNode find(List<JsonNode> obiekty, String nazwa) {
        return obiekty.stream().filter(o -> nazwa.equals(o.get("nazwa").asText())).findFirst()
                .orElseThrow(() -> new AssertionError("Nie znaleziono obiektu: " + nazwa));
    }

    @Test
    void nieWywalaSieNaWejsciachBrzegowych() {
        assertTrue(RaportParser.parse("", mapper).isEmpty());
        assertTrue(RaportParser.parse(null, mapper).isEmpty());
        assertTrue(RaportParser.parse("# Leady WH PLUS, 13.10.2026\n\nBrak.\n", mapper).isEmpty());
    }

    @Test
    void parsujeNaglowekZLokalizacjaITerminem() {
        String md = "## Zapytania ofertowe i przetargi\n\n"
                + "### 1. Hotel Bryza (Jurata, pomorskie)  ·  70.0 pkt\n"
                + "Typ: hotel · Inwestor: Jan Kowalski · Pokoje: 120\n\n"
                + "- 2026-10-01 · Przetarg na meble · Dostawa mebli do pokoi"
                + "  **Termin ofert: 2026-11-10** [BZP 123](https://ezamowienia.gov.pl/x)\n";
        List<JsonNode> o = RaportParser.parse(md, mapper);
        assertEquals(1, o.size());
        JsonNode x = o.get(0);
        assertEquals("Hotel Bryza", x.get("nazwa").asText());
        assertEquals("Jurata", x.get("miasto").asText());
        assertEquals("pomorskie", x.get("wojewodztwo").asText());
        assertEquals("hotel", x.get("rodzaj").asText());
        assertEquals("Jan Kowalski", x.path("szczegoly").path("inwestor").asText());
        assertEquals("120", x.path("szczegoly").path("liczba_pokoi").asText());
        JsonNode s = x.get("sygnaly").get(0);
        assertEquals("2026-11-10", s.get("termin").asText());
        assertEquals("Dostawa mebli do pokoi", s.get("opis").asText());
        assertEquals("BZP 123", s.get("zrodlo").asText());
    }
}
