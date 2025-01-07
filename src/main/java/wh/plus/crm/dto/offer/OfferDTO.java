package wh.plus.crm.dto.offer;

import lombok.Data;
import wh.plus.crm.dto.UserDTO;
import wh.plus.crm.dto.client.ClientSummaryDTO;
import wh.plus.crm.dto.lead.LeadSummaryDTO;
import wh.plus.crm.dto.project.ProjectSummaryDTO;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.RejectionReason;
import wh.plus.crm.model.offer.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OfferDTO {

    private Long id;
    private String clientGlobalId;


    private String name;
    private String description;
    private String rejectionReasonComment;
    private String approvalReason;
    private boolean isArchived;
    private boolean isContractSigned;
    private int version;

    private Currency currency;
    private RejectionReason rejectionReason;
    private ClientType clientType;
    private InvestorType investorType;
    private OfferStatus offerStatus;
    private ObjectType objectType;
    private SalesOpportunityLevel salesOpportunityLevel;
    private BigDecimal totalPriceInEUR;
    private BigDecimal euroExchangeRate;
    private BigDecimal totalPrice;


    private Long salesTeamId;
    private String salesTeamName;

    private LocalDateTime rejectionOrApprovalDate;
    private LocalDateTime signedContractDate;

    private List<OfferItemDTO> offerItems;
    private ProjectSummaryDTO project;
    private LeadSummaryDTO lead;
    private UserDTO user;
    private ClientSummaryDTO client;


    private String createdBy;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime creationDate;




}
