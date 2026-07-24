package wh.plus.crm.model.crew;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;

/** Członek ekipy monterskiej (nazwa + rola; nie musi być userem systemu). */
@Entity
@Table(name = "crew_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrewMember extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id")
    private Crew crew;

    private String name;

    private String role;
}
