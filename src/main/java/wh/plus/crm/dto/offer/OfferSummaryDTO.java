package wh.plus.crm.dto.offer;

import lombok.AllArgsConstructor;
import lombok.Data;
import wh.plus.crm.model.offer.OfferStatus;

@Data
@AllArgsConstructor

public class OfferSummaryDTO {

    private Long id;
    private String name;
    private OfferStatus offerStatus;

}
