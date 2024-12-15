package wh.plus.crm.dto.offer;

import lombok.Builder;
import lombok.Data;
import wh.plus.crm.model.Tax;

@Data
@Builder
public class OfferItemDTO {

    private Long id;
    private String title;
    private String description;
    private Double amount;
    private Long quantity;
    private Tax tax;
    private OfferDTO Offer;

}
