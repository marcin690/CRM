package wh.plus.crm.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EntityCreatedEvent<T> extends ApplicationEvent {

    private final T entity;
    private final String message;

    public EntityCreatedEvent(Object source, T entity, String message){
        super(source);
        this.entity = entity;
        this.message = message;
    }


}
