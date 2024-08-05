package wh.plus.crm.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import wh.plus.crm.dto.LeadDTO;
import wh.plus.crm.model.lead.Lead;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(target = "id", source = "id")
//    @Mapping(source = "createdBy.id", target = "createdBy")
    @Mapping(source = "assignTo.id", target = "assignTo")
    @Mapping(source = "leadStatus.id", target = "leadStatus")
    @Mapping(source = "contactInfo", target = "contactInfo")
    @Mapping(source = "leadSource.id", target = "leadSource")
    LeadDTO leadToLeadDTO(Lead lead);

    @Mapping(target = "id", source = "id")
//    @Mapping(target = "createdBy", ignore = true) // Ignoruj pełne obiekty podczas mapowania DTO na Lead
    @Mapping(target = "assignTo", ignore = true) // Ignoruj pełne obiekty podczas mapowania DTO na Lead
    @Mapping(target = "leadStatus", ignore = true) // Ignoruj pełne obiekty podczas mapowania DTO na Lead
    @Mapping(target = "contactInfo", ignore = true) // Ignoruj pełne obiekty podczas mapowania DTO na Lead
    @Mapping(target = "leadSource", ignore = true) // Ignoruj pełne obiekty podczas mapowania DTO na Lead
    Lead leadDTOtoLead(LeadDTO leadDTO);
}