package wh.plus.crm.model.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.user.User;

import java.time.LocalDate;

/**
 * Zadanie w etapie projektu — prosta checklista. Montaż jest jednym z typów zadań
 * (może być ciągły lub mieszany), z własnymi datami. Do etapu z montażem przypinamy zamówienia.
 */
@Entity
@Table(name = "stage_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StageTask extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id")
    private ProjectStage stage;

    private String title;

    /** Termin zadania. */
    private LocalDate dueDate;

    /** Status zadania (Nowy / W toku / Wstrzymany / Zakończony). */
    private String status;

    private boolean done;

    private Integer sortOrder;

    /** Zadanie prywatne — widoczne tylko dla twórcy (po createdBy z Auditable). */
    private boolean privateOnly;

    /** Szczegóły / opis zadania (rzadko używane — w UI schowane pod rozwinięciem). */
    @Column(length = 4000)
    private String details;

    /** Typ zadania — wolny tekst z podpowiedziami; „Montaż" odblokowuje tryb i daty montażu. */
    private String type;

    /** Tryb montażu: CIĄGŁY / MIESZANY (istotny tylko dla zadań typu Montaż). */
    private String montageMode;

    private LocalDate startDate;
    private LocalDate endDate;

    /** Osoba z biura odpowiedzialna za zadanie (przypisanie jest na poziomie zadania, nie etapu). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;
}
