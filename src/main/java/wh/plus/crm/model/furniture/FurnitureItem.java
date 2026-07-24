package wh.plus.crm.model.furniture;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.user.User;

import java.math.BigDecimal;

/**
 * Pozycja karty mebli (rozpiska / karta informacyjna — jeszcze NIE zamówienie).
 * Bardzo szczegółowa specyfikacja: mebel, lokalizacja, kod koloru/materiał, producent/podwykonawca,
 * wymiary, osoba odpowiedzialna, status ustaleń. Można ją wysłać do zamówienia / zlecić produkcję.
 */
@Entity
@Table(name = "furniture_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FurnitureItem extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Opcjonalne powiązanie z etapem ustaleń. */
    @Column(name = "stage_id")
    private Long stageId;

    /** Nazwa mebla (np. „Krzesło Montecarlo", „Materac", „Szafa sypialnia"). */
    private String name;

    /** Lokalizacja — pomieszczenie / apartament. */
    private String location;

    private Integer quantity = 1;

    /** Kod koloru / materiał (np. EGGER H3730, blat Kronospan). */
    private String colorCode;

    /** Producent lub podwykonawca. */
    private String producer;

    /** Wymiary (szer x wys x gł). */
    private String dimensions;

    /** Szczegóły / informacje dodatkowe. */
    @Column(length = 4000)
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    /** Status ustaleń (Do ustalenia / W ustaleniu / Ustalone / Zatwierdzone / Zamówione). */
    private String status;

    @Column(length = 2000)
    private String comment;

    /** Cena jednostkowa netto — informacyjnie (karta nie jest zamówieniem). */
    private BigDecimal unitPrice;

    private Integer sortOrder;

    /** Czy pozycja została już wysłana do zamówienia. */
    private Boolean ordered = false;
}
