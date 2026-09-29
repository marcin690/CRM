package wh.plus.crm.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kompletny zestaw agregatów dla cockpitu sprzedaży (strona /reports/cockpit).
 * Analiza obejmuje pełny cykl: lead, oferta (statusy, powody odrzucenia, segmenty), umowa.
 * Blok compare (opcjonalny) zawiera te same agregaty dla okresu porównawczego.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalyticsDTO {

    private Kpi kpi;
    private Kpi kpiCompare;              // KPI okresu porównawczego (null jeśli brak porównania)
    private Timing timing;
    private Funnel funnel;
    private Cycle cycle;                 // konwersje między etapami całego cyklu
    private OfferAnalytics offers;       // analityka ofert: statusy, win-rate, powody odrzucenia, segmenty
    private List<SourceRow> sources;
    private List<RepRow> reps;
    private Clients clients;
    private List<IndustryRow> industries;
    private List<TrendPoint> trend;
    private Heatmap reasonByIndustry;
    private Heatmap reasonByRep;
    private ComparePayload compare;      // pełne agregaty okresu porównawczego (null jeśli brak porównania)

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Kpi {
        private long leads;
        private long offers;
        private long signed;
        private double conversionRate;      // signed / leads * 100
        private BigDecimal pipelineValue;   // suma leadValue
        private BigDecimal avgContractValue;// średnia wartość podpisanej umowy
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Timing {
        private Integer leadToOfferDays;   // śr. dni od leada do pierwszej oferty
        private Integer offerToSignDays;   // śr. dni od utworzenia oferty do podpisania (ile trwa ofertowanie)
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Funnel {
        private long leads;
        private long offers;
        private Long sent;                  // oferty wysłane (status inny niż DRAFT); null gdy nieliczone (drill-down handlowca)
        private long accepted;              // oferty ACCEPTED lub SIGNED
        private long signed;                // oferty SIGNED z datą podpisu w okresie
    }

    /** Konwersje między etapami całego cyklu sprzedaży (kohorta: leady i oferty utworzone w okresie). */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Cycle {
        private long leads;
        private long offers;                 // wszystkie oferty utworzone w okresie
        private long offersSent;             // oferty, które wyszły poza szkic (SENT, ACCEPTED, REJECTED, SIGNED)
        private long offersAccepted;         // ACCEPTED lub SIGNED
        private long offersSigned;           // SIGNED (wg statusu kohorty)
        private long offersRejected;         // REJECTED
        private double leadToOfferRate;      // oferty / leady * 100
        private double offerToSentRate;      // wysłane / oferty * 100
        private double sentToAcceptedRate;   // zaakceptowane / wysłane * 100
        private double acceptedToSignedRate; // podpisane / zaakceptowane * 100
        private double winRate;              // SIGNED / (SIGNED + REJECTED) * 100
        private double overallRate;          // podpisane / leady * 100
    }

    /** Analityka ofert: rozkład statusów, win-rate, powody odrzucenia ofert, segmentacja. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OfferAnalytics {
        private List<StatusRow> statuses;        // rozkład wg statusu (pełna lista, także zerowe)
        private double winRate;                  // SIGNED / (SIGNED + REJECTED) * 100
        private long decided;                    // SIGNED + REJECTED (oferty rozstrzygnięte)
        private BigDecimal totalValue;           // suma wartości wszystkich ofert z okresu
        private BigDecimal avgOfferValue;        // średnia wartość oferty
        private List<ReasonRow> rejectionReasons;// powody odrzucenia OFERT (Offer.rejectionReason)
        private List<CategoryRow> objectTypes;   // segmentacja wg typu obiektu
        private List<CategoryRow> investorTypes; // segmentacja wg typu inwestora
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StatusRow {
        private String code;               // kod enuma (DRAFT, SENT, ...)
        private String name;               // opis PL
        private long count;
        private BigDecimal value;          // suma totalPrice
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ReasonRow {
        private String name;               // opis PL powodu
        private long count;
        private BigDecimal value;          // suma wartości utraconych ofert
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CategoryRow {
        private String name;               // opis PL (objectType / investorType)
        private long count;
        private BigDecimal value;          // suma wartości ofert
        private long signed;               // liczba podpisanych w segmencie
        private BigDecimal signedValue;    // wartość podpisanych w segmencie
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SourceRow {
        private String name;
        private long leads;
        private BigDecimal value;          // suma leadValue
        private double conversionRate;
        private long signed;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RepRow {
        private Long id;
        private String name;
        private String mainSource;
        private long leads;
        private BigDecimal value;
        private double conversionRate;
        private String topReason;          // najczęstszy powód odrzucenia (opis PL)
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Clients {
        private long newClients;
        private long returningClients;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class IndustryRow {
        private String name;               // opis ClientType (branża)
        private BigDecimal avgLeadValue;
        private long leads;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TrendPoint {
        private String month;              // etykieta np. "wrz 2026"
        private long leads;                // narastająco
        private long offers;              // narastająco
        private long signed;              // narastająco
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Heatmap {
        private List<String> reasons;      // etykiety kolumn (top N powodów)
        private List<HeatRow> rows;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class HeatRow {
        private String name;
        private List<Long> counts;         // liczności w kolejności `reasons`
        private long sum;                  // łączna liczba odrzuceń w wierszu (wszystkie powody)
    }

    /** Pełne agregaty okresu porównawczego, do zestawień A vs B na froncie. */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ComparePayload {
        private Kpi kpi;
        private Timing timing;
        private Funnel funnel;
        private Cycle cycle;
        private OfferAnalytics offers;
        private List<SourceRow> sources;
        private Clients clients;
    }
}
