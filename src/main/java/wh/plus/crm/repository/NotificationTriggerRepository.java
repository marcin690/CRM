package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.notification.NotificationTrigger;
import wh.plus.crm.model.notification.NotificationTriggerType;

import java.util.Optional;

public interface NotificationTriggerRepository extends JpaRepository<NotificationTrigger, Long> {

    Optional<NotificationTrigger> findByTriggerType(NotificationTriggerType triggerType);
}
