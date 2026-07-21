package wh.plus.crm.model.invoice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.project.Project;

@Entity
@Table(name = "project_fakturownia_binding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFakturowniaBinding extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private FakturowniaAccount account;

    private Long companyId;

    @Enumerated(EnumType.STRING)
    private FakturowniaRole role;

    private String fakturowniaCategoryName;

    /** Opcjonalne powiązanie kategorii z etapem projektu — faktury z tego wiązania liczą się do etapu. */
    @Column(name = "stage_id")
    private Long stageId;
}
