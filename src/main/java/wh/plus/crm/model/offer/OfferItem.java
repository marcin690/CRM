package wh.plus.crm.model.offer;
import com.sun.istack.NotNull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import wh.plus.crm.model.Tax;


@Entity
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title, description;

    @NotNull
    private Double amount;

    @NotNull
    private Long quantity;

    @Enumerated(EnumType.STRING)
    private Tax tax;

    @ManyToOne
    @JoinColumn(name = "offer_id")
    private Offer offer;


}
