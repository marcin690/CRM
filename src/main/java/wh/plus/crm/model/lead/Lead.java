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
import java.util.ArrayList;
import java.util.List;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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


    @Enumerated(EnumType.STRING)
    private ClientType clientType;

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

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Offer> offers = new ArrayList<>();


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lead_source_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private LeadSource leadSource;

    private String clientFullName, clientBusinessName, clientAdress, clientCity, clientState, clientZip, clientCountry, clientEmail;

    private Long clientPhone, vatNumber;

    /**
     * Czy lead pochodzi od klienta z którym wcześniej już współpracowaliśmy.
     * To atrybut leada, NIE kanał pozyskania — historycznie był jako LeadSource=RETURNING_CLIENT,
     * od V8 jest osobnym polem (vide migracja V8__Extract_returning_client_from_source.sql).
     * Nullable żeby odróżnić "nieustawione" od "fałsz" w starych rekordach poza backfillem.
     */
    @Column(name = "is_returning_client")
    private Boolean returningClient;

    /**
     * Pełny URL strony z której przyszedł lead (LP, formularz na www, itp.).
     * Pomaga rozróżnić różne landing-page i wersje formularzy w eksportach/raportach.
     */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    // ── Atrybucja marketingowa (Google Ads / UTM) — osobne kolumny pod raporty i BigQuery ──
    /** Identyfikator kliknięcia Google Ads (gclid) — klucz łączenia leada z kampanią w Ads/BigQuery. */
    @Column(name = "gclid", length = 512)
    private String gclid;

    @Column(name = "utm_source", length = 255)
    private String utmSource;

    @Column(name = "utm_medium", length = 255)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 255)
    private String utmCampaign;

    @Column(name = "utm_term", length = 255)
    private String utmTerm;

    @Column(name = "utm_content", length = 255)
    private String utmContent;

    @Override
    public String getClientGlobalId() {
        return clientGlobalId;
    }


}