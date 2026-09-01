package wh.plus.crm.service.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.notification.NotificationTriggerDTO;
import wh.plus.crm.model.notification.NotificationTrigger;
import wh.plus.crm.model.notification.NotificationTriggerType;
import wh.plus.crm.model.notification.NotificationType;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.NotificationTriggerRepository;
import wh.plus.crm.repository.UserRepository;
import wh.plus.crm.service.EmailService;
import wh.plus.crm.service.NotificationService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Silnik triggerów powiadomień.
 *
 * {@link #fire} wywołuje się w miejscach biznesowych (np. przyjęcie leada z formularza).
 * Metoda jest asynchroniczna i nigdy nie rzuca — wysyłka powiadomienia nie może
 * wywrócić operacji, która ją wyzwoliła.
 *
 * Reszta metod to CRUD konfiguracji dla panelu admina.
 */
@Service
@RequiredArgsConstructor
public class NotificationTriggerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTriggerService.class);

    private final NotificationTriggerRepository triggerRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /** Opcjonalny — bean istnieje tylko gdy skonfigurowany SMTP (spring.mail.host). */
    @Autowired(required = false)
    private EmailService emailService;

    /**
     * Rozsyła powiadomienie dla danego typu zdarzenia zgodnie z zapisaną konfiguracją.
     * Asynchroniczne + odporne na błędy — loguje, nie propaguje wyjątków.
     */
    @Async
    public void fire(NotificationTriggerType type, TriggerContext ctx) {
        try {
            NotificationTrigger trigger = triggerRepository.findByTriggerType(type).orElse(null);
            if (trigger == null || !trigger.isEnabled()) {
                log.debug("Trigger {} wyłączony lub nieskonfigurowany — pomijam.", type);
                return;
            }
            List<Long> recipientIds = new ArrayList<>(trigger.getRecipientUserIds());
            if (recipientIds.isEmpty()) {
                log.info("Trigger {} włączony, ale bez odbiorców — nic nie wysyłam.", type);
                return;
            }

            // Kanał in-app (dzwonek) — reużywa istniejący mechanizm powiadomień.
            if (trigger.isChannelInApp()) {
                notificationService.createImmediateNotification(
                        recipientIds,
                        NotificationType.IMMEDIATE,
                        false,   // e-mail obsługujemy osobno, dedykowanym szablonem
                        false,   // SMS nieużywany
                        false,
                        null,
                        ctx.inAppContent(),
                        ctx.entityType(),
                        ctx.entityId()
                );
            }

            // Kanał e-mail — dedykowany szablon; pomijamy gdy brak SMTP.
            if (trigger.isChannelEmail()) {
                if (emailService == null) {
                    log.warn("Trigger {} ma włączony e-mail, ale SMTP nie jest skonfigurowany (brak spring.mail.host).", type);
                } else {
                    for (User user : userRepository.findAllById(recipientIds)) {
                        if (user.getEmail() != null && !user.getEmail().isBlank()) {
                            emailService.sendEmail(user.getEmail(), ctx.emailSubject(), ctx.emailTemplate(), ctx.emailVariables());
                        }
                    }
                }
            }
            log.info("Trigger {} rozesłany do {} odbiorców (email={}, inApp={}).",
                    type, recipientIds.size(), trigger.isChannelEmail(), trigger.isChannelInApp());
        } catch (Exception e) {
            log.error("Błąd przy obsłudze triggera {}: {}", type, e.getMessage(), e);
        }
    }

    // ===== CRUD ustawień =====

    /** Zwraca konfigurację wszystkich typów triggerów (brakujące seeduje jako wyłączone). */
    @Transactional
    public List<NotificationTriggerDTO> getAll() {
        List<NotificationTriggerDTO> result = new ArrayList<>();
        for (NotificationTriggerType type : NotificationTriggerType.values()) {
            result.add(toDto(getOrCreate(type)));
        }
        return result;
    }

    @Transactional
    public NotificationTriggerDTO update(NotificationTriggerType type, NotificationTriggerDTO dto) {
        NotificationTrigger trigger = getOrCreate(type);
        trigger.setEnabled(dto.isEnabled());
        trigger.setChannelEmail(dto.isChannelEmail());
        trigger.setChannelInApp(dto.isChannelInApp());
        trigger.setRecipientUserIds(dto.getRecipientUserIds() != null
                ? new HashSet<>(dto.getRecipientUserIds())
                : new HashSet<>());
        return toDto(triggerRepository.save(trigger));
    }

    /** Zwraca istniejącą konfigurację lub tworzy domyślną (wyłączoną, in-app on). */
    @Transactional
    public NotificationTrigger getOrCreate(NotificationTriggerType type) {
        return triggerRepository.findByTriggerType(type).orElseGet(() ->
                triggerRepository.save(NotificationTrigger.builder()
                        .triggerType(type)
                        .enabled(false)
                        .channelEmail(true)
                        .channelInApp(true)
                        .build()));
    }

    private NotificationTriggerDTO toDto(NotificationTrigger t) {
        NotificationTriggerDTO dto = new NotificationTriggerDTO();
        dto.setTriggerType(t.getTriggerType());
        dto.setEnabled(t.isEnabled());
        dto.setChannelEmail(t.isChannelEmail());
        dto.setChannelInApp(t.isChannelInApp());
        dto.setRecipientUserIds(t.getRecipientUserIds());
        return dto;
    }
}
