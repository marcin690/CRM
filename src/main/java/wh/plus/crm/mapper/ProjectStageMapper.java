package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import wh.plus.crm.dto.project.ProjectStageDTO;
import wh.plus.crm.dto.project.StageTaskDTO;
import wh.plus.crm.model.project.ProjectStage;
import wh.plus.crm.model.project.StageTask;

@Mapper(componentModel = "spring")
public interface ProjectStageMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "responsibleUserId", source = "responsibleUser.id")
    @Mapping(target = "responsibleUserName", source = "responsibleUser.fullname")
    ProjectStageDTO toDto(ProjectStage entity);

    @Mapping(target = "responsibleUserId", source = "responsibleUser.id")
    @Mapping(target = "responsibleUserName", source = "responsibleUser.fullname")
    StageTaskDTO toTaskDto(StageTask task);

    // Zwykły update etapu NIE dotyka zamknięcia — tym zarządzają osobne akcje close/reopen.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "responsibleUser", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closedComment", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    void update(ProjectStageDTO dto, @MappingTarget ProjectStage entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stage", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "responsibleUser", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "clientGlobalId", ignore = true)
    void updateTask(StageTaskDTO dto, @MappingTarget StageTask entity);
}
