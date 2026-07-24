package wh.plus.crm.model.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;

/**
 * Komentarz (wątek) do wpisu w dzienniku budowy — np. odpowiedź klienta wklejona
 * przez zespół. Autor i data z {@link Auditable}.
 */
@Entity
@Table(name = "construction_log_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConstructionLogComment extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id")
    private ConstructionLogEntry entry;

    @Column(length = 4000, nullable = false)
    private String content;
}
