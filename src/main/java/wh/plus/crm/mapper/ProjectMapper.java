package wh.plus.crm.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import wh.plus.crm.dto.lead.LeadSummaryDTO;
import wh.plus.crm.dto.project.ProjectDTO;
import wh.plus.crm.dto.project.ProjectSummaryDTO;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.project.Project;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectDTO projectToProjectDTO(Project project);

    Project projectDTOToProject(ProjectDTO projectDTO);

    ProjectSummaryDTO toProjectSummaryDTO(Project project);




    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProjectFromProjectDTO(ProjectDTO projectDTO, @MappingTarget Project project);

}
