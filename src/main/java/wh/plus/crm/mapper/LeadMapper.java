package wh.plus.crm.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import wh.plus.crm.dto.LeadDTO;
import wh.plus.crm.model.lead.Lead;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(source = "createdBy.id", target = "createdBy")
    @Mapping(source = "assignTo.id", target = "assignTo")
    @Mapping(source = "leadStatus.id", target = "leadStatus")
    @Mapping(source = "contactInfo.id", target = "contactInfo")
    @Mapping(source = "leadSource.id", target = "leadSource")
    LeadDTO leadToLeadDTO(Lead lead);

    @Mapping(target = "createdBy.id", source = "createdBy")
    @Mapping(target = "assignTo.id", source = "assignTo")
    @Mapping(target = "leadStatus.id", source = "leadStatus")
    @Mapping(target = "contactInfo.id", source = "contactInfo")
    @Mapping(target = "leadSource.id", source = "leadSource")
    Lead leadDTOtoLead(LeadDTO leadDTO);
}
