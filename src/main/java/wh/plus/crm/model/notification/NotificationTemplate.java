package wh.plus.crm.model.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nazwa lub opis szablonu
    @Column(nullable = false)
    private String name;

    // Treść szablonu – może zawierać placeholdery do dynamicznej zamiany
    @Column(nullable = false)
    private String templateContent;

    // Typ powiadomienia, tutaj domyślnie BATCH
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    // Czy jest cykliczne – powinno być true
    @Column(nullable = false)
    private boolean cyclic;

    // Typ cykliczności, np. DAILY
    @Enumerated(EnumType.STRING)
    private CycleType cycleType;

    // Lista odbiorców – tutaj przykładowo zapis jako CSV identyfikatorów lub relacja ManyToMany
    @Column
    private String recipientIds;

    // Ustawienia dotyczące kanałów wysyłki
    private Boolean sendEmail;
    private Boolean sendSms;

    // Data rozpoczęcia aktywności szablonu
    @Column(nullable = true)
    private LocalDateTime startDate;


    // Pole do określenia kolejnego terminu wysyłki powiadomienia
    @Column(nullable = true)
    private LocalDateTime nextNotificationTime;


}
