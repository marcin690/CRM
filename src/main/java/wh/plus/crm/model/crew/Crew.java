package wh.plus.crm.model.crew;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ekipa monterska — byt na poziomie aplikacji (nie per projekt). Ma skład i kolor do harmonogramu (Gantt).
 */
@Entity
@Table(name = "crew")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Crew extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Brygadzista / osoba odpowiedzialna za montaż (tekst — nie musi być userem systemu). */
    private String foreman;

    /** Kontakt do osoby odpowiedzialnej za montaż. */
    private String email;

    private String phone;

    /** Kolor na osi harmonogramu (hex). */
    private String color;

    private boolean active = true;

    @OneToMany(mappedBy = "crew", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CrewMember> members = new ArrayList<>();
}
