package wh.plus.crm.model.lead;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import wh.plus.crm.helper.GenerateTemporaryClientId;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.User;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@EntityListeners(AuditingEntityListener.class)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@EnableJpaAuditing
@Audited
public class Lead extends Auditable<String>  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true)
    private String clientGlobalId;

    @Version
    private int version;


    @ManyToOne
    @JoinColumn(name = "assign_to_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private User assignTo;

    @ManyToOne()
    @JoinColumn(name = "lead_status_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private LeadStatus leadStatus;

    private String name;
    private Long leadValue, roomsQuantity;

    private LocalDateTime executionDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String leadRejectedReasonComment;


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