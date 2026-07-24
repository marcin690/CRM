package wh.plus.crm.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import wh.plus.crm.dto.project.ConstructionLogCommentDTO;
import wh.plus.crm.dto.project.ConstructionLogEntryDTO;
import wh.plus.crm.model.project.ConstructionLogComment;
import wh.plus.crm.model.project.ConstructionLogEntry;

@Mapper(componentModel = "spring")
public interface ConstructionLogEntryMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectStageId", source = "projectStage.id")
    @Mapping(target = "projectStageName", source = "projectStage.name")
    ConstructionLogEntryDTO toDto(ConstructionLogEntry entity);

    ConstructionLogCommentDTO toDto(ConstructionLogComment comment);

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "projectStage", ignore = true)
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    ConstructionLogEntry toEntity(ConstructionLogEntryDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "projectStage", ignore = true)
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    void update(ConstructionLogEntryDTO dto, @MappingTarget ConstructionLogEntry entity);
}
