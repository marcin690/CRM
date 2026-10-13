package wh.plus.crm.model.lead;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Pojedynczy przebieg agenta generującego kandydatów na leady (ręcznie wyzwalany). */
@Entity
@Table(name = "lead_candidate_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeadCandidateRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LeadCandidateRunStatus status = LeadCandidateRunStatus.RUNNING;

    private String tryb;

    @Column(name = "dni_newsy")
    private Integer dniNewsy;

    @Column(name = "dni_przetargi")
    private Integer dniPrzetargi;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** Liczba nowych kandydatów dodanych w tym przebiegu. */
    private Integer found;

    @Column(length = 2000)
    private String message;

    @Column(name = "started_by")
    private String startedBy;
}
