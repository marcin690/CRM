package wh.plus.crm.model.offer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.offer.Offer;

import java.time.LocalDateTime;

/**
 * Wygenerowany dokument oferty (PDF) — szkic od AI, poprawki handlowca i plik.
 * content = OfferPdfModel jako JSON; itemGroups = {offerItemId: "nazwa grupy"} (z tego kod liczy kwoty).
 */
@Entity
@Table(name = "offer_document")
@Getter
@Setter
@NoArgsConstructor
public class OfferDocument extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Offer offer;

    /** NEW | RETURNING */
    @Column(nullable = false, length = 16)
    private String clientType;

    @JsonRawValue
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @JsonRawValue
    @Lob
    @Column(name = "item_groups", columnDefinition = "TEXT")
    private String itemGroups;

    private String pdfUrl;

    /** Kolejny numer wygenerowanego PDF w obrębie oferty (1, 2, …) — część numeru oferty w PDF. */
    private Integer version;

    private LocalDateTime generatedAt;

    /** DRAFTING (AI pisze) | READY | RENDERING (PDF w toku) | FAILED (szkic się nie udał). */
    @Column(nullable = false, length = 16)
    private String status = "READY";

    /** Ostatni błąd przetwarzania w tle — pokazywany w kreatorze. */
    @Column(length = 1000)
    private String error;

    public Long getOfferId() { return offer == null ? null : offer.getId(); }
}
