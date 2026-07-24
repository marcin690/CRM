package wh.plus.crm.model.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Pozycja zamówienia — np. konkretny typ mebla, ilość i cena jednostkowa. */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private String name;

    /** Typ mebla / kategoria pozycji. */
    private String itemType;

    private Integer quantity = 1;

    @Column(precision = 19, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;
}
