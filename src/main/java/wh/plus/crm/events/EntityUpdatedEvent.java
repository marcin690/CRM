package wh.plus.crm.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EntityUpdatedEvent<T> extends ApplicationEvent {


    private final T entity;
    private final String message;

    public EntityUpdatedEvent(Object source, T entity,String message){
        super(source);
        this.entity = entity;
        this.message = message;
    }

}
