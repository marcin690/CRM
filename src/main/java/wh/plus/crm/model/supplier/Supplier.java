package wh.plus.crm.model.supplier;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;

/**
 * Dostawca — osobny zbiór bytów (rejestr dostawców), przypisywany do projektów.
 * Celowo niezależny od {@code User}: w przyszłości dostawca będzie mógł się logować,
 * ale na teraz trzymamy tylko dane podstawowe.
 */
@Entity
@Table(name = "supplier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Supplier extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String nip;
    private String email;
    private String phone;
    private String contactPerson;

    @Column(length = 500)
    private String address;

    @Column(length = 2000)
    private String notes;

    private boolean active = true;
}
