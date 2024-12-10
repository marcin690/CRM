package wh.plus.crm.mapper;

import org.mapstruct.*;
import wh.plus.crm.dto.CommentDTO;
import wh.plus.crm.model.comment.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(source = "lead.id", target = "leadId")
    @Mapping(source = "offer.id", target = "offerId")
    CommentDTO toDTO(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lead", ignore = true)
    @Mapping(source = "offerId", target = "offer.id")
    Comment toEntity(CommentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(CommentDTO dto, @MappingTarget Comment entity);

}
