package wh.plus.crm.dto.order;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Zestawienie finansowe projektu — rozdzielone na PLAN i REALIZACJĘ (odpowiednik arkusza KOSZTY).
 *
 * PLAN (wycena, statystyka): każda pozycja liczona DOKŁADNIE RAZ, bez dublowania —
 *   koszt planu = zamówienia + domówienia + koszty dodatkowe + montaże + meble jeszcze niewysłane do zamówienia.
 *   (Meble wysłane do zamówienia liczą się jako zamówienie, dlatego z planu mebli bierzemy tylko te niezamówione.)
 *
 * REALIZACJA: jedynym źródłem prawdy o przychodach i kosztach są faktury z Fakturowni (tylko odczyt).
 *   Oferty i zamówienia NIE są wiązane z fakturami — to porównanie statystyczne plan vs realizacja.
 */
@Data
public class ProjectStatsDTO {
    private Long projectId;
    private Long declaredValue;          // deklarowana wartość projektu (plan)
    private BigDecimal declaredMargin;   // marża deklarowana w % (informacyjnie)

    // --- PLAN / wycena (statystyka; każda pozycja liczona raz) ---
    private BigDecimal offersTotal;      // suma powiązanych ofert (informacyjnie, NIE wliczana do kosztu)
    private BigDecimal ordersTotal;      // suma zamówień (ORDER)
    private BigDecimal reordersTotal;    // suma domówień (REORDER)
    private BigDecimal additionalCostsTotal;  // koszty dodatkowe / stałe (przypięte do etapów)
    private BigDecimal montagesTotal;    // suma wartości montaży (encja Montage — jedyne źródło prawdy o montażu)
    private BigDecimal furnitureTotal;   // meble jeszcze niewysłane do zamówienia (unika dubla z zamówieniami)
    private BigDecimal planCostTotal;    // = zamówienia + domówienia + koszty dodatkowe + montaże + meble(niezamówione)
    private BigDecimal planMarginNet;    // = wartość deklarowana − koszt planu

    // --- REALIZACJA (jedyne źródło prawdy: faktury Fakturowni) ---
    private BigDecimal fakturowniaRevenueNet; // przychód z faktur sprzedażowych
    private BigDecimal fakturowniaCostNet;    // koszt z faktur zakupowych
    private BigDecimal realizationMarginNet;  // = przychód − koszt (faktury)

    // --- Aliasy wsteczne (semantyka REALIZACJI) ---
    private BigDecimal totalCostNet;     // = fakturowniaCostNet (koszt realizacji)
    private BigDecimal marginNet;        // = realizationMarginNet (marża realizacji)
}
