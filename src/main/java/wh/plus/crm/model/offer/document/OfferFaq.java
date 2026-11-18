package wh.plus.crm.model.offer.document;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pytanie z puli FAQ, z której handlowiec wybiera max 5 do oferty. Odpowiedź idzie do PDF bez AI. */
@Entity
@Table(name = "offer_faq")
@Getter
@Setter
@NoArgsConstructor
public class OfferFaq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String question;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    private int position;

    private boolean active = true;
}
