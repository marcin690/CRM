package wh.plus.crm.model.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

/**
 * Konfiguracja jednego triggera powiadomień (jedno zdarzenie = jeden wiersz).
 *
 * Trzyma: czy trigger jest włączony, jakimi kanałami wysyłać (e-mail / in-app)
 * oraz listę odbiorców (ID użytkowników CRM). Edytowalne z panelu admina.
 *
 * Tabela tworzona automatycznie przez Hibernate (ddl-auto=update) — bez migracji
 * Flyway, bo operacja jest czysto addytywna.
 */
@Entity
@Table(name = "notification_triggers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Typ zdarzenia — unikatowy, jeden wiersz konfiguracji na typ.
     * Mapowany jako VARCHAR (nie natywny ENUM), żeby dodanie kolejnych typów
     * triggerów nie wymagało zmiany schematu bazy.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, length = 64)
    private NotificationTriggerType triggerType;

    /** Czy trigger jest aktywny. Wyłączony => fire() nic nie robi. */
    @Column(nullable = false)
    private boolean enabled;

    /** Kanał e-mail. */
    @Column(nullable = false)
    private boolean channelEmail;

    /** Kanał powiadomień w aplikacji (dzwonek). */
    @Column(nullable = false)
    private boolean channelInApp;

    /** ID użytkowników-odbiorców. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "notification_trigger_recipients",
            joinColumns = @JoinColumn(name = "trigger_id")
    )
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> recipientUserIds = new HashSet<>();
}
