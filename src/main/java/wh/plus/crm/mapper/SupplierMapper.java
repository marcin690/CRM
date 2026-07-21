package wh.plus.crm.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import wh.plus.crm.dto.supplier.SupplierDTO;
import wh.plus.crm.model.supplier.Supplier;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    SupplierDTO toDto(Supplier entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "clientGlobalId", ignore = true)
    Supplier toEntity(SupplierDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "clientGlobalId", ignore = true)
    void update(SupplierDTO dto, @MappingTarget Supplier entity);
}
