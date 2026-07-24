package wh.plus.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import wh.plus.crm.dto.invoice.InvoiceDTO;
import wh.plus.crm.model.invoice.Invoice;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountLabel", source = "account.label")
    @Mapping(target = "bindingId", source = "binding.id")
    @Mapping(target = "role", source = "binding.role")
    InvoiceDTO toDto(Invoice entity);
}
