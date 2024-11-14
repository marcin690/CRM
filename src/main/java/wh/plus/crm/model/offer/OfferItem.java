package wh.plus.crm.model.offer;
import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import wh.plus.crm.model.Tax;

@Entity
@Audited
public class OfferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title, description;
    private Long amount, quantity;

    @Enumerated(EnumType.STRING)
    private Tax tax;

    @ManyToOne
    @JoinColumn(name = "offer_id")
    private Offer offer;


}
