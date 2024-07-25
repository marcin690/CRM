package wh.plus.crm.model.lead;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lead_source")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeadSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name, description;

}
