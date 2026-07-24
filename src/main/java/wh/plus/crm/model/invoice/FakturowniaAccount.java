package wh.plus.crm.model.invoice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;

@Entity
@Table(name = "fakturownia_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FakturowniaAccount extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;

    private String apiSubdomain;

    @Column(length = 512)
    private String apiToken;

    private boolean enabled = true;
}
