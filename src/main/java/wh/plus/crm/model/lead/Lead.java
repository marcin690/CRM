package wh.plus.crm.model.lead;

import com.fasterxml.jackson.annotation.*;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.user.User;
import org.hibernate.envers.Audited;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.RejectionReason;

import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@EntityListeners(AuditingEntityListener.class)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@EnableJpaAuditing
@Audited
public class Lead extends Auditable<String>  {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true)
    private String clientGlobalId;


    private boolean isFinal;

    @Version
    private int version;


    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private RejectionReason rejectionReason;

    private String rejectionReasonComment;



    @ManyToOne
    @JoinColumn(name = "user_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User user;

    @ManyToOne()
    @JoinColumn(name = "lead_status_id")
    @Nullable
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private LeadStatus leadStatus;

    private String name;
    private Long roomsQuantity;
    private Double leadValue;

    private LocalDateTime executionDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String leadRejectedReasonComment;

    @OneToOne(mappedBy = "lead", orphanRemoval = false, cascade = CascadeType.DETACH)
    private Offer offer;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lead_source_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private LeadSource leadSource;

    private String clientFullName, clientBusinessName, clientAdress, clientCity, clientState, clientZip, clientCountry, clientEmail;

    private Long clientPhone, vatNumber;

    @Override
    public String getClientGlobalId() {
        return clientGlobalId;
    }


}