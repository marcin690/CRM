package wh.plus.crm.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.dto.offer.OfferSummaryDTO;
import wh.plus.crm.model.user.User;
import wh.plus.crm.model.RejectionReason;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadDTO {

    private Long id;
    private String clientId;
    private int version;

    private String clientGlobalId;
    private boolean isFinal;
    private OfferSummaryDTO offer;

    private RejectionReason rejectionReason;
    private String rejectionReasonComment;



    private User user;
    private Long leadStatusId;
    private String name;
    private Double leadValue;
    private Long roomsQuantity;
    private LocalDateTime executionDate;
    private String description;
    private String leadRejectedReasonComment;
    private Long leadSourceId;

    private String clientFullName, clientBusinessName, clientAdress, clientCity, clientState, clientZip, clientCountry, clientEmail;

    private Long clientPhone, vatNumber;

    private String createdBy;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime creationDate;
}
