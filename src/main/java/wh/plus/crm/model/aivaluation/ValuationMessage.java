package wh.plus.crm.model.aivaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pojedyncza wiadomość w wątku czatu wyceny (użytkownik ↔ agent Dify).
 */
@Entity
@Table(name = "valuation_message")
@Getter
@Setter
@NoArgsConstructor
public class ValuationMessage {

    public enum Role { USER, ASSISTANT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ValuationJob job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** message_id z Dify (dla odpowiedzi agenta) — potrzebne do wysłania oceny do Dify. */
    private String difyMessageId;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
