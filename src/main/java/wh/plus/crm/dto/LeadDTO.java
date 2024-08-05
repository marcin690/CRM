package wh.plus.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.model.contactInfo.ContactInfo;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadDTO {

    private Long id;
    private int version;

    private Long assignTo;
    private Long leadStatus;
    private String name;
    private Long leadValue;
    private Long roomsQuantity;
    private LocalDateTime executionDate;
    private String description;
    private String leadRejectedReasonComment;
    private ContactInfo contactInfo;
    private Long leadSource;

}
