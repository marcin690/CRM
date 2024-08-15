package wh.plus.crm.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import wh.plus.crm.events.EntityCreatedEvent;
import wh.plus.crm.model.Notification;
import wh.plus.crm.model.User;
import wh.plus.crm.model.comment.EntityType;
import wh.plus.crm.repository.NotificationRepository;
import wh.plus.crm.repository.UserRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Component
public class EntityEventListener {


    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @EventListener
    public void handleEntityCreatedEvent(EntityCreatedEvent<?> event){
        createNotidications(event.getEntity(), event.getMessage());
    }

    private void createNotidications(Object entity, String message) {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            createNotification(message, user, entity);
        }
    }

    private void createNotification(String message, User user, Object entity) {
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setUser(user);
        notification.setEntityId(getEntityId(entity));
        notification.setEntityType(EntityType.valueOf(entity.getClass().getSimpleName().toUpperCase()));
        notification.setTimestamp(LocalDateTime.now());

        notificationRepository.save(notification);

    }

    private Long getEntityId(Object entity){
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return (Long) field.get(entity);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get entity ID", e);
        }}


    private String generateEntityUrl(Object entity){
        Long entityId = getEntityId(entity);
        String entityType = entity.getClass().getSimpleName().toLowerCase();

        return String.format("/%ss/%d", entityType, entityId);
    }


}
