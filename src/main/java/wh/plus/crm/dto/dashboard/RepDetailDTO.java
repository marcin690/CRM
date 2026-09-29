package wh.plus.crm.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Drill-down: pełny rozkład jednego handlowca za wybrany okres. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepDetailDTO {
    private Long id;
    private String name;
    private SalesAnalyticsDTO.Kpi kpi;
    private SalesAnalyticsDTO.Timing timing;
    private SalesAnalyticsDTO.Funnel funnel;
    private List<SalesAnalyticsDTO.SourceRow> sources;
    private List<ReasonCount> reasons;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ReasonCount {
        private String reason;
        private long count;
    }
}
