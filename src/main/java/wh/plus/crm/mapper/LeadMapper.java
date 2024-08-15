package wh.plus.crm.mapper;

import org.mapstruct.*;
import wh.plus.crm.dto.LeadDTO;
import wh.plus.crm.model.lead.Lead;

@Mapper(componentModel = "spring")
public interface LeadMapper {


    @Mapping(source = "createdBy", target = "createdBy")
    @Mapping(source = "creationDate", target = "creationDate")
    @Mapping(source = "lastModifiedBy", target = "lastModifiedBy")
    @Mapping(source = "lastModifiedDate", target = "lastModifiedDate")
    Lead leadDTOtoLead(LeadDTO leadDTO);

    LeadDTO leadToLeadDTO(Lead lead);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateLeadFromDto(LeadDTO leadDTO, @MappingTarget Lead lead);
}
