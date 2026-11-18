package wh.plus.crm.service.offerdoc;

import org.junit.jupiter.api.Test;
import wh.plus.crm.dto.offer.OfferPdfModel.Pricing;
import wh.plus.crm.model.Tax;
import wh.plus.crm.model.offer.OfferItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfferPricingTest {

    private static OfferItem item(long id, String amount, long qty, Tax tax) {
        OfferItem i = new OfferItem();
        i.setId(id);
        i.setTitle("poz " + id);
        i.setAmount(new BigDecimal(amount));
        i.setQuantity(qty);
        i.setTax(tax);
        return i;
    }

    @Test
    void sumsPerGroupAndTotalFromItemsNotFromAi() {
        OfferItem incomplete = new OfferItem();
        incomplete.setId(4L); // brak kwoty → pomijana, jak w OfferService
        List<OfferItem> items = List.of(
                item(1, "1000.00", 116, Tax.VAT_23),
                item(2, "250.50", 2, Tax.VAT_23),
                item(3, "99.99", 1, null),
                incomplete);
        // AI wskazało nieznaną grupę dla 3 → trafia do "Inne"
        Map<Long, String> groups = Map.of(1L, "Pokoje", 2L, "Restauracja", 3L, "Spa");

        Pricing p = OfferDocumentService.buildPricing(items, groups, List.of("Pokoje", "Restauracja", "Inne"), "PLN");

        assertEquals(3, p.groups().size());
        assertEquals(new BigDecimal("116000.00"), p.groups().get(0).net());
        assertEquals(new BigDecimal("142680.00"), p.groups().get(0).gross());
        assertEquals("Inne", p.groups().get(2).name());
        assertEquals(new BigDecimal("116600.99"), p.totalNet());
        assertEquals(new BigDecimal("143396.22"), p.totalGross());
    }
}
