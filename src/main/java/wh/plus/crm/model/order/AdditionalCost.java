package wh.plus.crm.model.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.project.Project;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Koszt dodatkowy projektu wpisywany z palca — POZA Fakturownią (transport, nocleg, itp.).
 * Dane Fakturowni nigdy nie są modyfikowane; to są nasze własne pozycje kosztowe.
 */
@Entity
@Table(name = "project_additional_cost")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalCost extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Typ kosztu (Transport / Nocleg / Inne) — tekst dla uniwersalności. */
    private String type;

    private String description;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    private String currency = "PLN";

    private LocalDate costDate;

    /** Etap, którego dotyczy koszt (luźne id, spójnie z Order.stageId). */
    @Column(name = "stage_id")
    private Long stageId;
}
