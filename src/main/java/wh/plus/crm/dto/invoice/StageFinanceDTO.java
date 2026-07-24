package wh.plus.crm.dto.invoice;

import lombok.Data;

import java.math.BigDecimal;

/** Finanse per etap — z faktur, których wiązanie Fakturowni wskazuje na dany etap. */
@Data
public class StageFinanceDTO {
    private Long stageId;
    private BigDecimal revenueNet = BigDecimal.ZERO;
    private BigDecimal costNet = BigDecimal.ZERO;
    private BigDecimal marginNet = BigDecimal.ZERO;
    private int invoiceCount;
}
