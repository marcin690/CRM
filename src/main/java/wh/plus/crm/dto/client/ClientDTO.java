package wh.plus.crm.dto.client;
import lombok.*;
import wh.plus.crm.dto.ContactDTO;
import wh.plus.crm.dto.EventDTO;
import wh.plus.crm.dto.offer.OfferSummaryDTO;
import wh.plus.crm.dto.project.ProjectSummaryDTO;


import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClientDTO {

    private Long id;
    private String clientFullName;
    private String clientBusinessName;
    private String clientAdress;
    private String clientCity;
    private String clientState;
    private String clientZip;
    private String clientCountry;
    private String clientEmail;
    private String clientNotes;
    private Long clientPhone;
    private Long vatNumber;
    private String fakturowniaCategory;
    private int version;
    private List<ContactDTO> contacts;
    private List<EventDTO> events;
    private List<OfferSummaryDTO> offers;
    private List<ProjectSummaryDTO> projects;

    private String createdBy;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime creationDate;


}
