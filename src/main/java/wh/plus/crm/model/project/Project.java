package wh.plus.crm.model.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.Contact;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.invoice.ProjectFakturowniaBinding;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.supplier.Supplier;
import wh.plus.crm.model.user.SalesTeam;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Project extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<Offer> offers = new ArrayList<>();

    // Trzy kolekcje poniżej są wyłączone z Envers (@NotAudited).
    // Powód: targety (ProjectFakturowniaBinding, ConstructionLogEntry, ProjectStage)
    // nie są @Audited, a @Audited(targetAuditMode = NOT_AUDITED) na @OneToMany
    // i tak rzuca EnversMappingException w Hibernate 6.5.
    // Dla Offer (też @OneToMany) to działa, bo Offer SAM jest @Audited.

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<ProjectFakturowniaBinding> fakturowniaBindings = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<ConstructionLogEntry> constructionLog = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<ProjectStage> stages = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "project_supplier",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "supplier_id"))
    @NotAudited
    private List<Supplier> suppliers = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_team_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private SalesTeam salesTeam;

    private Long roomQuantity, projectNetValue;

    // Czy jest etapowy
    private boolean isStage, isSimple;

    @ManyToOne
    @JoinColumn(name = "contact_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Contact contacts;

    // Dodatkowe pola Long
    /**
     * Łączna deklarowana wartość projektu
     */
    private Long totalDeclaredValue;

    /** Marża deklarowana (procent), do której dąży projekt. */
    private java.math.BigDecimal declaredMargin;

    /** Miasto / lokalizacja realizacji (do harmonogramu montaży). */
    private String city;

    /** Status projektu w pipeline (Wysłane / Zaakceptowane / W realizacji / Zrealizowane / Wstrzymane). */
    private String status;

    /**
     * Ilość pięter
     */
    private Long floorCount;

    // Dodatkowe pola Boolean
    /**
     * Możliwość dojazdu samochodu ciężarowego
     */
    private boolean isTruckAccessible;

    /**
     * Praca w weekend
     */
    private boolean isWeekendWork;

    /**
     * Praca w godzinach nocnych
     */
    private boolean isNightWork;

    /**
     * Czy dodane w realizacjach na stronie
     */
    private boolean isAddedToWebsite;

    /**
     * Czy dodane w social mediach
     */
    private boolean isAddedToSocialMedia;

    /**
     * Czy otrzymano list referencyjny
     */
    private boolean isReferenceLetterReceived;

    // Dodatkowe pola String
    /**
     *  Typ ścian
     */
    private String wallType;

    /**
     * Możliwość noclegu
     */
    private String accommodationOption;

    /**
     * Opis projektu
     */
    private String projectDescription;

    /**
     * Dane do faktury
     */
    private String invoiceDetails;

    /**
     * Adres dostawy
     */
    private String deliveryAddress;
}
