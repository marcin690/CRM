package wh.plus.crm.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Lekki przegląd etapów projektu — do wizualizacji postępu na liście projektów. */
@Data
public class ProjectStageSummaryDTO {
    private Long projectId;
    private int total;
    private int closed;
    /** Realizacja finansowa w % (przychód z Fakturowni vs wartość deklarowana). */
    private java.math.BigDecimal financialPercent;
    private List<Stage> stages = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stage {
        private String name;
        private Integer sortOrder;
        private boolean closed;
    }
}
