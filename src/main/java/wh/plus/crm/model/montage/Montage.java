package wh.plus.crm.model.montage;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.crew.Crew;
import wh.plus.crm.model.project.Project;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Montaż jako osobny byt — jednostka planowania na osi czasu. Spięty z projektem (i opcjonalnie etapem),
 * przypisany do ekipy monterskiej, z terminem i trybem. Podstawa harmonogramu / widoku Gantt.
 */
@Entity
@Table(name = "montage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Montage extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Opcjonalne powiązanie z etapem (luźne id, spójnie z Order.stageId). */
    @Column(name = "stage_id")
    private Long stageId;

    private String name;

    private String address;

    /** Tryb montażu: CIĄGŁY / MIESZANY. */
    private String mode;

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id")
    private Crew crew;

    /** Wartość montażu netto — wliczana do kosztów projektu. */
    private BigDecimal netValue;

    /** Status montażu (Zaplanowany / W toku / Zakończony / Wstrzymany). */
    private String status;

    @Column(length = 2000)
    private String notes;
}
