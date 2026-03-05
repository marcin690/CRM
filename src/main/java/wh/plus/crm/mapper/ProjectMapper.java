package wh.plus.crm.mapper;

import org.mapstruct.*;
import wh.plus.crm.dto.offer.OfferSummaryDTO;
import wh.plus.crm.dto.project.ProjectDTO;
import wh.plus.crm.dto.project.ProjectSummaryDTO;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.project.Project;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {ClientMapper.class})
public interface ProjectMapper {

    @Mapping(target = "salesTeamId", source = "salesTeam.id")
    @Mapping(target = "salesTeamName", source = "salesTeam.name")
    @Mapping(target = "offers", expression = "java(mapOffersToSummary(project.getOffers()))")
    ProjectDTO projectToProjectDTO(Project project);

    @Mapping(target = "salesTeam.id", source = "salesTeamId")
    @Mapping(target = "offers", ignore = true)
    @Mapping(target = "client", ignore = true)
    Project projectDTOToProject(ProjectDTO projectDTO);

    ProjectSummaryDTO toProjectSummaryDTO(Project project);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "salesTeam", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "offers", ignore = true)
    void updateProjectFromProjectDTO(ProjectDTO projectDTO, @MappingTarget Project project);

    default List<OfferSummaryDTO> mapOffersToSummary(List<Offer> offers) {
        if (offers == null) return Collections.emptyList();
        return offers.stream()
                .map(o -> new OfferSummaryDTO(o.getId(), o.getName(), o.getOfferStatus(), o.getTotalPrice()))
                .collect(Collectors.toList());
    }
}
