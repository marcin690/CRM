package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import wh.plus.crm.dto.order.OrderDTO;
import wh.plus.crm.dto.order.OrderItemDTO;
import wh.plus.crm.model.order.Order;
import wh.plus.crm.model.order.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "projectTeamName", source = "project.salesTeam.name")
    OrderDTO toDto(Order entity);

    OrderItemDTO toItemDto(OrderItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "clientGlobalId", ignore = true)
    void update(OrderDTO dto, @MappingTarget Order entity);
}
