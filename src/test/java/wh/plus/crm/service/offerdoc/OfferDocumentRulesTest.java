package wh.plus.crm.service.offerdoc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OfferDocumentRulesTest {

    @Test
    void missingMarkerCaughtInAnyCase() {
        for (String t : new String[]{"[uzupełnij: liczba pokoi]", "[Uzupełnij: termin]", "[ UZUPEŁNIJ ]", "[do uzupełnienia]", "[uzupelnij: x]"}) {
            assertTrue(OfferDocumentService.MISSING.matcher("Mamy " + t + " pokoi").find(), t);
        }
        assertFalse(OfferDocumentService.MISSING.matcher("Uzupełnimy dokumentację do końca stycznia.").find());
    }

    @Test
    void incompleteAiAnswerIsAnError() throws Exception {
        ObjectMapper om = new ObjectMapper();
        assertThrows(IllegalStateException.class, () -> OfferDocumentService.requireComplete(om.readTree("{}")));
        assertThrows(IllegalStateException.class, () -> OfferDocumentService.requireComplete(om.missingNode()));
        assertThrows(IllegalStateException.class, () -> OfferDocumentService.requireComplete(
                om.readTree("{\"headline\":\"x\",\"situation\":[\"a\"],\"groups\":[]}")));
        assertDoesNotThrow(() -> OfferDocumentService.requireComplete(
                om.readTree("{\"headline\":\"x\",\"situation\":[\"a\"],\"groups\":[{\"itemId\":1,\"group\":\"Pokoje\"}]}")));
    }

    @Test
    void wpNameMatchIgnoresCaseAndSpaces() {
        assertEquals(WpContactClient.normalize("Rafał  Hyla"), WpContactClient.normalize(" rafał hyla "));
    }

    @Test
    void missingMarkerCheckedOnlyInAiFields() {
        var faq = java.util.List.of(new wh.plus.crm.dto.offer.OfferPdfModel.Qa("Termin?", "8 tygodni"));
        var clean = new wh.plus.crm.dto.offer.OfferPdfModel("NEW", "[uzupełnij]", "K", 1L, "d", null, java.util.List.of(),
                java.util.List.of(), "Wyposażenie 10 pokoi", java.util.List.of("ok"), "cel", java.util.List.of(), java.util.List.of(),
                java.util.List.of(), null, java.util.List.of(), java.util.List.of(), faq, java.util.List.of(), java.util.List.of(), null, null);
        assertFalse(OfferDocumentService.hasMissing(clean)); // nazwa projektu nie jest w edytorze — nie blokuje
        var withGap = new wh.plus.crm.dto.offer.OfferPdfModel("NEW", "P", "K", 1L, "d", null, java.util.List.of(),
                java.util.List.of(), "x", java.util.List.of("Chcecie [Uzupełnij: liczba pokoi] pokoi"), "cel", java.util.List.of(),
                java.util.List.of(), java.util.List.of(), null, java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), null, null);
        assertTrue(OfferDocumentService.hasMissing(withGap));
        var faqGap = new wh.plus.crm.dto.offer.OfferPdfModel("NEW", "P", "K", 1L, "d", null, java.util.List.of(),
                java.util.List.of(), "x", java.util.List.of("ok"), "cel", java.util.List.of(), java.util.List.of(), java.util.List.of(),
                null, java.util.List.of(), java.util.List.of(),
                java.util.List.of(new wh.plus.crm.dto.offer.OfferPdfModel.Qa("Termin?", "[do uzupełnienia]")),
                java.util.List.of(), java.util.List.of(), null, null);
        assertTrue(OfferDocumentService.hasMissing(faqGap)); // FAQ jest w edytorze z czerwoną ramką, więc też blokuje
    }

    @Test
    void offerNumberAndValidityDate() {
        assertEquals("WH/12/3", OfferDocumentService.offerNumber(12L, 3));
        assertEquals("WH/12", OfferDocumentService.offerNumber(12L, null)); // dokumenty sprzed numeracji wersji
        assertEquals("2026-12-19", OfferDocumentService.validDate(" 2026-12-19 "));
        assertNull(OfferDocumentService.validDate("19.12.2026"));
        assertNull(OfferDocumentService.validDate(""));
        var today = java.time.LocalDate.of(2026, 11, 19);
        assertTrue(OfferDocumentService.expired("2026-11-18", today));
        assertFalse(OfferDocumentService.expired("2026-11-19", today)); // ważna jeszcze w dniu ważności
        assertFalse(OfferDocumentService.expired(null, today));
    }

    @Test
    void scopeIsDeduplicatedAndNeverEmpty() {
        assertEquals(java.util.List.of("Pokoje", "Restauracja"),
                OfferDocumentService.cleanScope(java.util.Arrays.asList("Pokoje", " Pokoje ", "", null, "Restauracja")));
        assertEquals(java.util.List.of("Inne"), OfferDocumentService.cleanScope(null));
    }

    @Test
    void acfFalseIsNotPrintedAsText() throws Exception {
        var acf = new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"telefon\": false, \"e-mail\": \" a@b.pl \"}");
        assertNull(WpContactClient.text(acf, "telefon"));
        assertEquals("a@b.pl", WpContactClient.text(acf, "e-mail"));
    }
}
