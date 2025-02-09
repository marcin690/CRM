package wh.plus.crm.mapper;

import org.mapstruct.*;
import wh.plus.crm.dto.lead.LeadDTO;
import wh.plus.crm.dto.lead.LeadSummaryDTO;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.repository.LeadSourceRepository;
import wh.plus.crm.repository.LeadStatusRepository;

import java.util.NoSuchElementException;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(target = "clientGlobalId", ignore = true)
    @Mapping(source = "createdBy", target = "createdBy")
    @Mapping(source = "creationDate", target = "creationDate")
    @Mapping(source = "lastModifiedBy", target = "lastModifiedBy")
    @Mapping(source = "lastModifiedDate", target = "lastModifiedDate")
    @Mapping(target = "leadStatus", ignore = true)
    @Mapping(target = "leadSource", ignore = true)
    Lead leadDTOtoLead(LeadDTO leadDTO, @Context LeadStatusRepository leadStatusRepository, @Context LeadSourceRepository leadSourceRepository);

    @Mapping(source = "clientGlobalId", target = "clientId")
    @Mapping(source = "leadStatus.id", target = "leadStatusId")
    @Mapping(source = "leadSource.id", target = "leadSourceId")
    @Mapping(source = "offers", target = "offers")
    LeadDTO leadToLeadDTO(Lead lead);

    LeadSummaryDTO toLeadSummaryDTO(Lead lead);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateLeadFromDto(LeadDTO leadDTO, @MappingTarget Lead lead, @Context LeadStatusRepository leadStatusRepository, @Context LeadSourceRepository leadSourceRepository);

    @AfterMapping
    default void mapLeadStatus(LeadDTO leadDTO, @MappingTarget Lead lead, @Context LeadStatusRepository leadStatusRepository ){
        if(leadDTO.getLeadStatusId() != null){
            lead.setLeadStatus(leadStatusRepository.findById(leadDTO.getLeadStatusId()).orElseThrow(() -> new NoSuchElementException("Lead Ststus not found")));
        }
    }

    @AfterMapping
    default void mapLeadSource(LeadDTO leadDTO, @MappingTarget Lead lead, @Context LeadSourceRepository leadSourceRepository){
        if(leadDTO.getLeadSourceId() != null){
            lead.setLeadSource(leadSourceRepository.findById(leadDTO.getLeadSourceId()).orElseThrow(() -> new NoSuchElementException("Lead Source not found")));
        }
    }
}
