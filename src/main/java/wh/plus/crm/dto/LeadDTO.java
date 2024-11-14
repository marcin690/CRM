package wh.plus.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.model.User;

import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.model.lead.LeadStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadDTO {

    private Long id;
    private String clientId;
    private int version;

    private User assignTo;
    private Long leadStatusId;
    private String name;
    private Long leadValue;
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
