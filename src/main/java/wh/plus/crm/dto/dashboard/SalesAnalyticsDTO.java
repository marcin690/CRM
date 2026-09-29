package wh.plus.crm.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kompletny zestaw agregatów dla cockpitu sprzedaży (strona /reports/cockpit).
 * Jedno zapytanie zwraca wszystkie wymiary analizy lead → oferta → umowa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalyticsDTO {

    private Kpi kpi;
    private Kpi kpiCompare;              // opcjonalnie: ten sam zestaw KPI dla okresu porównawczego (null jeśli brak)
    private Timing timing;
    private Funnel funnel;
    private List<SourceRow> sources;
    private List<RepRow> reps;
    private Clients clients;
    private List<IndustryRow> industries;
    private List<TrendPoint> trend;
    private Heatmap reasonByIndustry;
    private Heatmap reasonByRep;

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
        private long accepted;              // oferty ACCEPTED lub SIGNED
        private long signed;               // oferty SIGNED z datą podpisu w okresie
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
}
