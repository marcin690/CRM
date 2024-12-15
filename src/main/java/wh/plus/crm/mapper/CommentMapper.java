package wh.plus.crm.mapper;

import org.mapstruct.*;
import wh.plus.crm.dto.CommentDTO;
import wh.plus.crm.model.comment.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(source = "lead.id", target = "leadId")
    @Mapping(source = "offer.id", target = "offerId")
    @Mapping(source = "client.id", target = "clientGlobalId")
    CommentDTO toDTO(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lead", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "offer", ignore = true)
    Comment toEntity(CommentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(CommentDTO dto, @MappingTarget Comment entity);

}
