package wh.plus.crm.dto.offer;

import lombok.Builder;
import lombok.Data;
import wh.plus.crm.model.Tax;

import java.math.BigDecimal;

@Data
@Builder
public class OfferItemDTO {

    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private Long quantity;
    private Tax tax;
    private OfferDTO Offer;
    private BigDecimal grossAmount;
    private BigDecimal taxAmount;

}
