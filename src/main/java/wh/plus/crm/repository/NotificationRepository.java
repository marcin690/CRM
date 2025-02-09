package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.notification.Notification;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndOpenFalse(Long userId);

    List<Notification> findByOpenTrueAndCreationDateBefore(LocalDateTime dateTime);

}
