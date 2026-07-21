package wh.plus.crm.model.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;

/**
 * Prosty komentarz / notatka przypięta do projektu — niezależny od dziennika budowy.
 * Autor i data pochodzą z {@link Auditable}.
 */
@Entity
@Table(name = "project_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectComment extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(length = 4000, nullable = false)
    private String content;
}
