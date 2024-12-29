package wh.plus.crm.model.offer;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.istack.NotNull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import wh.plus.crm.model.Tax;

import java.math.BigDecimal;


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
    private BigDecimal amount;

    @NotNull
    private Long quantity;

    @Enumerated(EnumType.STRING)
    private Tax tax;

    @Column(nullable = true, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = true, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @ManyToOne
    @JoinColumn(name = "offer_id")
    @JsonBackReference
    @JsonIgnore
    private Offer offer;


}
