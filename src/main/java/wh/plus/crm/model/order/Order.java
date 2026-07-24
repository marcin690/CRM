package wh.plus.crm.model.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.supplier.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Zamówienie powiązane z projektem. Uniwersalne (CRM hotelowy): typ jest tekstem
 * (Meble / AGD / Oświetlenie / ...), a szczegóły są w pozycjach {@link OrderItem}.
 * Tabela nazwana {@code project_order}, bo "order" to słowo zarezerwowane w SQL.
 */
@Entity
@Table(name = "project_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    private String name;

    /** Uniwersalny typ zamówienia (Meble / AGD / Oświetlenie / Tekstylia / Inne). */
    private String type;

    @Enumerated(EnumType.STRING)
    private OrderKind orderKind = OrderKind.ORDER;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.DRAFT;

    private String currency = "PLN";

    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;

    /**
     * Wartość netto zamówienia w płaskim modelu (wpisywana wprost, jak w arkuszu „Zamówienia").
     * Gdy ustawiona, ma pierwszeństwo nad sumą pozycji przy liczeniu zestawień.
     */
    private BigDecimal netValue;

    /**
     * Luźne powiązanie z etapem projektu (Faza 1 — pełny model etapów/zadań dojdzie później).
     * Trzymane jako id, żeby nie wymuszać twardego FK zanim etapy mają swoje API.
     */
    @Column(name = "stage_id")
    private Long stageId;

    /** Przypięty plik lub katalog z SharePointa projektu (link + nazwa do wyświetlenia). */
    private String sharepointUrl;
    private String sharepointName;

    @Column(length = 2000)
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();
}
