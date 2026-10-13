package wh.plus.crm.model.lead;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Kandydat na leada wygenerowany przez agenta AI (Dify) na podstawie przetargów
 * BZP/TED, sygnałów inwestycyjnych i zapytań ofertowych.
 *
 * <p>Handlowiec przegląda kandydatów kartami (swipe) i decyduje: Dodaj
 * ({@link LeadCandidateStatus#ACCEPTED} + utworzenie wersji roboczej leada)
 * albo Odrzuć ({@link LeadCandidateStatus#REJECTED}). {@code dedupKey} zapobiega
 * ponownemu pojawianiu się tego samego obiektu w kolejnych przebiegach.
 */
@Entity
@Table(name = "lead_candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeadCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Znormalizowany klucz nazwa|miasto — unikalny, do deduplikacji między przebiegami. */
    @Column(name = "dedup_key", unique = true, length = 400)
    private String dedupKey;

    private String nazwa;
    private String miasto;
    private String wojewodztwo;
    private String rodzaj;

    private int punkty;
    private boolean przetarg;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LeadCandidateStatus status = LeadCandidateStatus.NEW;

    /** Surowa tablica JSON sygnałów (renderowana na froncie). */
    @Column(name = "sygnaly_json", columnDefinition = "TEXT")
    private String sygnalyJson;

    /** Surowy obiekt JSON szczegółów (inwestor, operator, liczba pokoi, …). */
    @Column(name = "szczegoly_json", columnDefinition = "TEXT")
    private String szczegolyJson;

    /** Pierwszy link źródłowy (skrót do „Otwórz źródło"). */
    @Column(name = "zrodlo_url", length = 1000)
    private String zrodloUrl;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decided_by")
    private String decidedBy;

    /** Id utworzonego leada po akceptacji. */
    @Column(name = "accepted_lead_id")
    private Long acceptedLeadId;

    @Column(name = "rejection_note", length = 1000)
    private String rejectionNote;
}
