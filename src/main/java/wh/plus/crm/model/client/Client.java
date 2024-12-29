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
import wh.plus.crm.model.Contact;
import wh.plus.crm.model.Event;
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
public class Client extends Auditable<String>   {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientFullName, clientBusinessName, clientAdress, clientCity, clientState, clientZip, clientCountry, clientEmail;

    private String clientNotes;

    private Long clientPhone, vatNumber;

    private String fakturowniaCategory;

    @Version
    private int version;

    @OneToMany(mappedBy = "client")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<Contact> contacts;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "client")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<Event> events;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED) // Wyłącza audytowanie tej relacji
    private List<Offer> offers;


    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST)
    private List<Project> projects;



}
