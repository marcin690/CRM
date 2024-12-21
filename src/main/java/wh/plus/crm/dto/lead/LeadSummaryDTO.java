package wh.plus.crm.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.model.offer.Offer;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadSummaryDTO {

    private Long id;
    private String name;
    private LeadStatus leadStatus;



}
