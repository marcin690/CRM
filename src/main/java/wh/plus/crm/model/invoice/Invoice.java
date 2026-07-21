package wh.plus.crm.model.invoice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lokalna kopia faktury pobranej z Fakturowni (read-only cache).
 * System NIGDY nie modyfikuje faktur w Fakturowni — ta encja jest tylko snapshotem.
 */
@Entity
@Table(name = "invoice",
        uniqueConstraints = @UniqueConstraint(name = "uk_invoice_fakturownia",
                columnNames = {"fakturownia_id", "account_id"}),
        indexes = {
                @Index(name = "ix_invoice_binding", columnList = "binding_id"),
                @Index(name = "ix_invoice_issue_date", columnList = "issue_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fakturownia_id", nullable = false)
    private Long fakturowniaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private FakturowniaAccount account;

    /**
     * Wiązanie z projektem przez konto Fakturowni + rolę. Nullable + ON DELETE
     * SET NULL — usunięcie wiązania nie kasuje historii faktur w cache.
     * Faktura osierocona staje się "nieprzypisana" i znika z agregacji finansów,
     * ale fizycznie pozostaje w bazie.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "binding_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ProjectFakturowniaBinding binding;

    private Long companyId;

    private String number;

    /** Tytuł faktury (opcjonalny opis z Fakturowni). */
    private String title;

    @Column(name = "issue_date")
    private LocalDate issueDate;
    private LocalDate sellDate;
    private LocalDate paymentDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal netValue;
    @Column(precision = 19, scale = 2)
    private BigDecimal grossValue;
    @Column(precision = 19, scale = 2)
    private BigDecimal vatValue;

    private String currency;

    @Enumerated(EnumType.STRING)
    private InvoiceKind kind;

    private String status;

    private boolean paid;

    private String buyerName;
    private String sellerName;

    @Column(length = 1024)
    private String fakturowniaUrl;

    private LocalDateTime lastSyncedAt;
}
