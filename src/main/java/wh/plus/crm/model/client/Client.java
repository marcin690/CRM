package wh.plus.crm.model.client;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.ContactInfoList;
import wh.plus.crm.model.Events;
import wh.plus.crm.model.common.HasClientId;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.project.Project;

import java.util.List;


@Entity
@Table(name = "clients")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@EnableJpaAuditing
@Audited
public class Client extends Auditable<String> implements HasClientId  {

    @Id
    private Long id;
    private String clientFullName, clientBusinessName, clientAdress, clientCity, clientState, clientZip, clientCountry, clientEmail;

    private String clientNotes;

    private Long clientPhone, vatNumber;

    private String fakturowniaCategory;

    @Version
    private int version;

    @ElementCollection
    @CollectionTable(name = "clients_contact_info", joinColumns = @JoinColumn(name = "client"))
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<ContactInfoList> contactInfoListList;

    @ElementCollection
    @CollectionTable(name = "clients_events", joinColumns = @JoinColumn(name = "client"))
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<Events> events;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED) // Wyłącza audytowanie tej relacji
    private List<Offer> offers;

    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST)
    private List<Project> projects;
























}
