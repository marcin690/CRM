package wh.plus.crm.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import wh.plus.crm.dto.order.AdditionalCostDTO;
import wh.plus.crm.model.order.AdditionalCost;

@Mapper(componentModel = "spring")
public interface AdditionalCostMapper {

    AdditionalCostDTO toDto(AdditionalCost entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "clientGlobalId", ignore = true)
    AdditionalCost toEntity(AdditionalCostDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "clientGlobalId", ignore = true)
    void update(AdditionalCostDTO dto, @MappingTarget AdditionalCost entity);
}
