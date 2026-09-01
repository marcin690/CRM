package wh.plus.crm.dto.notification;

import lombok.Data;
import wh.plus.crm.model.notification.NotificationTriggerType;

import java.util.HashSet;
import java.util.Set;

/** DTO konfiguracji triggera — używane przez panel admina (GET/PUT /notifications/triggers). */
@Data
public class NotificationTriggerDTO {

    private NotificationTriggerType triggerType;
    private boolean enabled;
    private boolean channelEmail;
    private boolean channelInApp;
    private Set<Long> recipientUserIds = new HashSet<>();
}
