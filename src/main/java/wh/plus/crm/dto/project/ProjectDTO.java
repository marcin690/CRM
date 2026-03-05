package wh.plus.crm.dto.project;

import lombok.Data;
import wh.plus.crm.dto.client.ClientSummaryDTO;
import wh.plus.crm.dto.offer.OfferSummaryDTO;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectDTO {

    private Long id;
    private String name;

    private ClientSummaryDTO client;

    private Long salesTeamId;
    private String salesTeamName;

    private Long roomQuantity;
    private Long projectNetValue;

    private String projectDescription;

    private List<OfferSummaryDTO> offers;

    private String createdBy;
    private LocalDateTime creationDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
