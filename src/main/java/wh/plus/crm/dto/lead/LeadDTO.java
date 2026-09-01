package wh.plus.crm.dto.lead;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.dto.offer.OfferSummaryDTO;
import wh.plus.crm.model.lead.ClientType;
import wh.plus.crm.model.user.User;
import wh.plus.crm.model.RejectionReason;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadDTO {

    private Long id;
    private String clientId;
    private int version;

    private String clientGlobalId;
    private boolean isFinal;
    private List<OfferSummaryDTO> offers;

    private RejectionReason rejectionReason;
    private String rejectionReasonComment;


    private ClientType clientType;

    private User user;
    private Long leadStatusId;
    private String name;
    private Double leadValue;
    private Long roomsQuantity;
    private LocalDateTime executionDate;
    private String description;
    private String leadRejectedReasonComment;
    private Long leadSourceId;

    private String clientFullName, clientBusinessName,
            clientAdress, clientCity, clientState, clientZip, clientCountry, clientEmail;

    private Long clientPhone, vatNumber;

    /** Klient powracający — atrybut leada, nie kanał pozyskania (vide V8). */
    private Boolean returningClient;

    /** Pełny URL strony z której przyszedł lead (np. LP /wycena). */
    private String sourceUrl;

    /** Atrybucja marketingowa (Google Ads / UTM) — do raportów i eksportu do BigQuery. */
    private String gclid, utmSource, utmMedium, utmCampaign, utmTerm, utmContent;

    private String createdBy;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime creationDate;
}
