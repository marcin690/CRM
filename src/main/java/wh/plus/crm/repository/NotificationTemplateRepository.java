package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.notification.CycleType;
import wh.plus.crm.model.notification.NotificationTemplate;
import wh.plus.crm.model.notification.NotificationType;

import java.util.List;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    List<NotificationTemplate> findByNotificationTypeAndCyclic(NotificationType notificationType, boolean cyclic);
    List<NotificationTemplate> findByNotificationTypeAndCyclicAndCycleType(NotificationType notificationType, boolean cyclic, CycleType cycleType);


}
