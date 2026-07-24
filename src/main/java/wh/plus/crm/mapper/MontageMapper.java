package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import wh.plus.crm.dto.montage.MontageDTO;
import wh.plus.crm.model.montage.Montage;

@Mapper(componentModel = "spring")
public interface MontageMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "projectCity", source = "project.city")
    @Mapping(target = "crewId", source = "crew.id")
    @Mapping(target = "crewName", source = "crew.name")
    @Mapping(target = "crewColor", source = "crew.color")
    MontageDTO toDto(Montage entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "crew", ignore = true)
    void update(MontageDTO dto, @MappingTarget Montage entity);
}
