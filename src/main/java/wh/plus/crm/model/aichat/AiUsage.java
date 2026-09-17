package wh.plus.crm.model.aichat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Zużycie AI — jeden wiersz na wiadomość (z {@code message_end.metadata.usage}).
 * Podstawa raportów admina (tokeny/koszt per użytkownik). NIE zawiera treści rozmów.
 */
@Entity
@Table(name = "ai_usage")
@Getter
@Setter
@NoArgsConstructor
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Kopia emaila na dzień użycia — do raportów, nawet po zmianie emaila usera. */
    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiChatApp app;

    @Column(length = 64)
    private String model;

    /** Chroni przed podwójnym zapisem tej samej wiadomości. */
    @Column(name = "dify_message_id", nullable = false, unique = true, length = 64)
    private String difyMessageId;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    /** Koszt liczony po stronie CRM (na razie = to, co zwróciło Dify). */
    @Column(name = "cost_usd", precision = 12, scale = 6)
    private BigDecimal costUsd;

    /** Surowa cena z Dify (bywa 0). */
    @Column(name = "dify_total_price", precision = 12, scale = 6)
    private BigDecimal difyTotalPrice;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
