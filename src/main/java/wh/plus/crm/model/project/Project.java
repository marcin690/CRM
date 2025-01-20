package wh.plus.crm.model.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.Contact;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.offer.Offer;

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
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<Offer> offers = new ArrayList<>();


    private Long roomQuantity, projectNetValue;

    // Czy jest etapowy
    private boolean isStage, isSimple;

    @ManyToOne
    @JoinColumn(name = "contact_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Contact contacts;

    private String fakturowniaCategory;

    // Dodatkowe pola Long
    /**
     * Łączna deklarowana wartość projektu
     */
    private Long totalDeclaredValue;

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
