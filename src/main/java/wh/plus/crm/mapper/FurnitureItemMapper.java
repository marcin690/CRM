package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import wh.plus.crm.dto.furniture.FurnitureItemDTO;
import wh.plus.crm.model.furniture.FurnitureItem;

@Mapper(componentModel = "spring")
public interface FurnitureItemMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "responsibleUserId", source = "responsibleUser.id")
    @Mapping(target = "responsibleUserName", source = "responsibleUser.fullname")
    FurnitureItemDTO toDto(FurnitureItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "responsibleUser", ignore = true)
    @Mapping(target = "ordered", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    void update(FurnitureItemDTO dto, @MappingTarget FurnitureItem entity);
}
