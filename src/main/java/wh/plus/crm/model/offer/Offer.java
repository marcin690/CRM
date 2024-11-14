package wh.plus.crm.model.offer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.User;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.common.HasClientId;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.project.Project;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "offers")
@EntityListeners(AuditingEntityListener.class)
@Data
@Audited
public class Offer extends Auditable<String> implements HasClientId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name, desctiption;

    private boolean isUpdated;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private OfferStatus offerStatus;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferItem> offerItemList;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "lead")
    private Lead lead;

    @ManyToOne
    @JoinColumn(name = "client")
    private Client client;





}
