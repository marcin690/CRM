package wh.plus.crm.model.aichat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Rozmowa AI należąca do JEDNEGO użytkownika. CRM trzyma tylko własność i metadane —
 * treść wiadomości jest w Dify (źródło prawdy historii).
 *
 * <p>Izolacja: każdy odczyt/zapis filtruje po {@code ownerUserId}. {@code difyConversationId}
 * nigdy nie trafia do frontendu — front operuje wyłącznie lokalnym {@code id}.
 */
@Entity
@Table(name = "ai_conversation")
@Getter
@Setter
@NoArgsConstructor
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Właściciel — id z tabeli users. Wszystkie zapytania są zawężone do tej wartości. */
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    /** Który czat (CLAUDE/CHATGPT/GEMINI). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiChatApp app;

    /** conversation_id z Dify — null do pierwszej odpowiedzi. Nie wychodzi poza backend. */
    @Column(name = "dify_conversation_id", length = 64, unique = true)
    private String difyConversationId;

    @Column(length = 255)
    private String title;

    /** Ostatni task_id ze streamu — do zatrzymania generowania (POST /chat-messages/:task/stop). */
    @Column(name = "last_task_id", length = 64)
    private String lastTaskId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Soft-delete — rozmowa znika z list, ale wiersze ai_usage zostają dla raportów. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
