package wh.plus.crm.dto.invoice;

import lombok.Data;
import wh.plus.crm.model.invoice.FakturowniaRole;
import wh.plus.crm.model.invoice.InvoiceKind;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvoiceDTO {
    private Long id;
    private Long fakturowniaId;
    private Long accountId;
    private String accountLabel;
    private Long bindingId;
    private FakturowniaRole role;
    private Long companyId;
    private String number;
    private String title;
    private LocalDate issueDate;
    private LocalDate sellDate;
    private LocalDate paymentDate;
    private BigDecimal netValue;
    private BigDecimal grossValue;
    private BigDecimal vatValue;
    private String currency;
    private InvoiceKind kind;
    private String status;
    private boolean paid;
    private String buyerName;
    private String sellerName;
    private String fakturowniaUrl;
}
