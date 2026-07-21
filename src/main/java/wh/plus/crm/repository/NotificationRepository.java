package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.notification.Notification;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndOpenFalse(Long userId);

    List<Notification> findByOpenTrueAndCreationDateBefore(LocalDateTime dateTime);

    // --- Dzwonek: powiadomienia zalogowanego użytkownika ---
    List<Notification> findTop30ByUser_IdOrderByIdDesc(Long userId);

    long countByUser_IdAndOpenFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.open = true where n.user.id = :userId and n.open = false")
    void markAllRead(@Param("userId") Long userId);
}
