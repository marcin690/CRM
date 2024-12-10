package wh.plus.crm.mapper;
import org.mapstruct.*;
import wh.plus.crm.dto.EventDTO;
import wh.plus.crm.model.Event;


@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "clientId", source = "client.id")
    EventDTO eventToEventDTO(Event event);

    Event eventDTOTOEvent (EventDTO eventDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromEventDTO(EventDTO eventDTO, @MappingTarget Event event);



}
