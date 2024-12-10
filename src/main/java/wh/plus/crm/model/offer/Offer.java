package wh.plus.crm.model.offer;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.User;
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
@Audited
public class Offer extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name, rejectionReasonComment;
    private String approvalReason;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean isArchived;

    private int version;

    @Enumerated(EnumType.STRING)
    private Currency currency;

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

    private LocalDateTime rejectionOrApprovalDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User user;


    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferItem> offerItemList = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;


}
