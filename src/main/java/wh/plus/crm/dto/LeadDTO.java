package wh.plus.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.model.User;
import wh.plus.crm.model.contactInfo.ContactInfo;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.model.lead.LeadStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadDTO {

    private Long id;
    private int version;

    private User assignTo;
    private LeadStatus leadStatus;
    private String name;
    private Long leadValue;
    private Long roomsQuantity;
    private LocalDateTime executionDate;
    private String description;
    private String leadRejectedReasonComment;
    private ContactInfo contactInfo;
    private LeadSource leadSource;

    private String createdBy;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime creationDate;
}
