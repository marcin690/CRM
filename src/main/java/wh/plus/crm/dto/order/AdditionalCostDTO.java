package wh.plus.crm.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdditionalCostDTO {
    private Long id;
    private String type;
    private String description;
    private BigDecimal amount;
    private String currency;
    private LocalDate costDate;
    private Long stageId;
    private String createdBy;
}
