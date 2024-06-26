package wh.plus.crm.model.lead;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import wh.plus.crm.model.LeadOfferRejectedReason;
import wh.plus.crm.model.User;
import wh.plus.crm.model.contactInfo.ContactInfo;

import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@EnableJpaAuditing
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    @JsonIgnoreProperties({"username", "password", "email", "fullname", "phone", "roles", "leads", "assignedLeads", "enabled", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "lead_status_id")
    private LeadStatus leadStatus;

    private String name;
    private Long leadValue;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private String leadRejectedReasonComment;

    @ManyToOne
    @JoinColumn(name = "rejected_reason_id", nullable = true)
    private LeadOfferRejectedReason leadRejectedReason;

    @ManyToOne
    @JoinColumn(name = "assigned_to_id", nullable = true)
    @JsonIgnoreProperties({"username", "password", "email", "phone", "roles", "leads", "assignedLeads", "enabled", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
    private User assignedTo;

    @ManyToOne
    @JoinColumn(name = "contact_info_id")
    private ContactInfo contactInfo;





}
