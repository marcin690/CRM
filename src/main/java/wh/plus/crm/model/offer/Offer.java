package wh.plus.crm.model.offer;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.RejectionReason;
import wh.plus.crm.model.user.User;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.project.Project;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offers")
@EntityListeners(AuditingEntityListener.class)
@Data
@ToString(exclude = {"offerItemList"})
@Audited
public class Offer extends Auditable<String> {

    @Id
//   @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = true)
    private String approvalReason, rejectionReasonComment;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean isArchived;

    @Column(name = "is_contract_signed")
    private boolean contractSigned;

    private int version;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private RejectionReason rejectionReason;

    @Enumerated(EnumType.STRING)
    private ClientType clientType;

    @Enumerated(EnumType.STRING)
    private InvestorType investorType;


    @Enumerated(EnumType.STRING)
    private OfferStatus offerStatus;

    @Enumerated(EnumType.STRING)
    private ObjectType objectType;

    @Enumerated(EnumType.STRING)
    private SalesOpportunityLevel salesOpportunityLevel;

    @Column(nullable = true, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = true, precision = 19, scale = 2)
    private BigDecimal totalPriceInEUR; // W euro

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal euroExchangeRate;

    @Column(nullable = true)
    private LocalDateTime rejectionOrApprovalDate;

    @Column(nullable = true)
    private LocalDateTime signedContractDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User user;


    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OfferItem> offerItemList = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;


   @ManyToOne
   @JoinColumn(name = "lead_id", nullable = true)
   private Lead lead;


    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;


}
