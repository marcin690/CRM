package wh.plus.crm.model.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.model.EntityType;
import wh.plus.crm.model.user.User;

import java.time.LocalDateTime;

/**
 * Encja będzie reprezentować powiadomienie systemowe zapisane w bazie. Dla uproszczenia przyjmujemy, że dla każdego odbiorcy (użytkownika) tworzymy osobny rekord. W encji znajdziemy m.in. treść powiadomienia, typ powiadomienia, flagę czy jest cykliczne, informacje o powiązanej encji, datę utworzenia oraz status „przeczytane”.
 *
 *
 * user – dzięki relacji many-to-one wiemy, do którego użytkownika należy powiadomienie.
 * notificationType – określa, czy powiadomienie ma być przetwarzane natychmiast (np. wysyłka e-mail/SMS) czy zbiorczo.
 * cyclic oraz cycleType – umożliwiają określenie, czy powiadomienie ma się powtarzać i z jaką częstotliwością.
 * relatedEntityType i relatedEntityId – pozwalają powiązać powiadomienie z dowolną encją (np. event, komentarz).
 */
@Data
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /**
     * The unique identifier for the notification.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    LocalDateTime creationDate;

    /**
     * The user associated with the notification.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The content of the notification.
     */
    private String content;

    /**
     * The type of the notification.
     */
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    /**
     * Indicates if the notification is cyclic.
     */
    private Boolean cyclic;

    /**
     * The cycle type of the notification if it is cyclic.
     */
    @Enumerated(EnumType.STRING)
    private CycleType cycleType;

    /**
     * The type of the related entity.
     */
    @Enumerated(EnumType.STRING)
    private EntityType relatedEntityType;

    /**
     * The ID of the related entity.
     */
    private Long relatedEntityId;

    /**
     * Indicates if the notification has been read.
     */
    private boolean open = false;

    private Boolean sendEmail;
    private Boolean sendSms;

    // Pole określające termin, kiedy ma być wysłane kolejne powiadomienie (dla cyklicznych)
    private LocalDateTime nextNotificationTime;



}