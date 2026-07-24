package wh.plus.crm.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import wh.plus.crm.dto.invoice.ProjectFakturowniaBindingDTO;
import wh.plus.crm.model.invoice.ProjectFakturowniaBinding;

@Mapper(componentModel = "spring")
public interface ProjectFakturowniaBindingMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountLabel", source = "account.label")
    ProjectFakturowniaBindingDTO toDto(ProjectFakturowniaBinding entity);

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "account", ignore = true)
    ProjectFakturowniaBinding toEntity(ProjectFakturowniaBindingDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "account", ignore = true)
    void update(ProjectFakturowniaBindingDTO dto, @MappingTarget ProjectFakturowniaBinding entity);
}
