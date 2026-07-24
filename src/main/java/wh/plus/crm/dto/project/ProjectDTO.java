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

    /** Identyfikator cyklu życia klienta (lead → oferta → projekt → klient). Tylko do odczytu — nadaje go system. */
    private String clientGlobalId;

    private ClientSummaryDTO client;

    private Long salesTeamId;
    private String salesTeamName;

    private Long roomQuantity;
    private Long floorCount;
    private Long projectNetValue;
    private java.math.BigDecimal declaredMargin;
    private String city;
    private String status;

    // Warunki realizacji (boxed — częściowy PATCH nie wyzeruje pozostałych).
    private Boolean truckAccessible;
    private Boolean weekendWork;
    private Boolean nightWork;
    private Boolean referenceLetterReceived;
    private Boolean addedToWebsite;
    private Boolean addedToSocialMedia;

    private String projectDescription;

    private List<OfferSummaryDTO> offers;

    private String createdBy;
    private LocalDateTime creationDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
