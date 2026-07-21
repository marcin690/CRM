package wh.plus.crm.dto.montage;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MontageDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private String projectCity;
    private Long stageId;
    private String name;
    private String address;
    private String mode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long crewId;
    private String crewName;
    private String crewColor;
    private BigDecimal netValue;
    private String status;
    private String notes;
}
