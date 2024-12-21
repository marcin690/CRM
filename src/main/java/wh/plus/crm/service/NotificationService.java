package wh.plus.crm.service;

import org.springframework.stereotype.Service;
import wh.plus.crm.model.EntityType;
import wh.plus.crm.model.Notification;
import wh.plus.crm.repository.NotificationRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;


    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getUserNotifications(Long userId){
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotifications(Long userid){
        return notificationRepository.findByUserIdAndIsOpenedFalse(userid);
    }

    public void markAsRead(Long notificationId){
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Notyfication not found"));

        notification.setOpened(true);
        notificationRepository.save(notification);


    }



}
