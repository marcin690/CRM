package wh.plus.crm.model.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_stage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStage extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    private String name;

    private LocalDate startDate;
    private LocalDate endDate;

    private String status;

    /** Kolejność etapu na osi projektu (etapy z szablonu bywają bez dat — potrzebna stabilna kolejność). */
    private Integer sortOrder;

    /** Osoba z biura odpowiedzialna za etap. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    @Column(length = 4000)
    private String notes;

    /** Zamknięcie etapu: data i komentarz podsumowujący (etap jako złożony moduł, nie tylko status). */
    private LocalDateTime closedAt;

    @Column(length = 2000)
    private String closedComment;

    /** Prosta checklista zadań w etapie (montaż jest jednym z zadań). */
    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<StageTask> tasks = new ArrayList<>();
}
