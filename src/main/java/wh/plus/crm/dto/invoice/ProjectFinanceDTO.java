package wh.plus.crm.dto.invoice;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectFinanceDTO {
    private Long projectId;
    private Long declaredValue;
    private BigDecimal sumRevenueNet;
    private BigDecimal sumCostNet;
    private BigDecimal sumRevenueGross;
    private BigDecimal sumCostGross;
    private BigDecimal completionPercent;
    private BigDecimal marginNet;
    private List<InvoiceDTO> invoices;
}
