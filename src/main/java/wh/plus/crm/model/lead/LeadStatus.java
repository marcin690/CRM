package wh.plus.crm.model.lead;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lead_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadStatus {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String statusName, statusDescription;

    @Enumerated(EnumType.STRING)
    private StatusType statusType;

    private boolean isFinal;

    public enum StatusType{
        POSITIVE, NEGATIVE, NEUTRAL
    }
}
