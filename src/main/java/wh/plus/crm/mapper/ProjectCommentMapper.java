package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import wh.plus.crm.dto.project.ProjectCommentDTO;
import wh.plus.crm.model.project.ProjectComment;

@Mapper(componentModel = "spring")
public interface ProjectCommentMapper {
    ProjectCommentDTO toDto(ProjectComment entity);
}
