package wh.plus.crm.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.LeadDTO;
import wh.plus.crm.events.EntityCreatedEvent;
import wh.plus.crm.mapper.LeadMapper;
import wh.plus.crm.model.contactInfo.ContactInfo;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.repository.*;
import wh.plus.crm.model.User;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LeadService {

    private static final Logger logger = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadStatusRepository leadStatusRepository;
    private final LeadSourceRepository leadSourceRepository;

    private final ContactInfoRepository contactInfoRepository;
    private final LeadMapper leadMapper;
    private final UserService userService;

    private final ApplicationEventPublisher applicationEventPublisher;




    public List<LeadDTO> findAll() {
       return leadRepository.findAll().stream().map(leadMapper::leadToLeadDTO).collect(Collectors.toList());
    }

    public Optional<LeadDTO> findById(Long id) {
        return leadRepository.findById(id)
                .map(leadMapper::leadToLeadDTO);
    }

    @Transactional
    public LeadDTO save(LeadDTO leadDTO) {
        Lead lead = leadMapper.leadDTOtoLead(leadDTO);
        String username = userService.getCurrentUsername();

//         Optional<User> user = userRepository.findByUsername(username);
//         if(user.isPresent()) {
//             lead.setCreatedBy(user.get());
//         } else {
//             throw new NoSuchElementException("User not found");
//         }

         if(leadDTO.getLeadStatus() != null){
            LeadStatus leadStatus = leadStatusRepository.findById(leadDTO.getLeadStatus())
                    .orElseThrow(() -> new NoSuchElementException("LeadStatus not found"));
            lead.setLeadStatus(leadStatus);
         }

         if(leadDTO.getLeadSource() != null){
             LeadSource leadSource = leadSourceRepository.findById(leadDTO.getLeadSource())
                     .orElseThrow(() -> new NoSuchElementException("Lead Source not found"));
             lead.setLeadSource(leadSource);
         }

        if (leadDTO.getContactInfo() != null && leadDTO.getContactInfo().getId() != null) {
            ContactInfo contactInfo = contactInfoRepository.findById(leadDTO.getContactInfo().getId())
                    .orElseThrow(() -> new NoSuchElementException("Contact info not found"));
            lead.setContactInfo(contactInfo);
        } else {
            lead.setContactInfo(leadDTO.getContactInfo());
        }

         if(leadDTO.getAssignTo() != null) {
             User assignedUser = userRepository.findById(leadDTO.getAssignTo())
                     .orElseThrow(() -> new NoSuchElementException("User not fund"));
             lead.setAssignTo(assignedUser);

         }

         Lead savedLead = leadRepository.save(lead);

        String message = String.format("Użytkownik %s dodał lead o nazwie %s", username, savedLead.getName());
        applicationEventPublisher.publishEvent(new EntityCreatedEvent<>(this, savedLead, message));



         return leadMapper.leadToLeadDTO(savedLead);




   }


    @Transactional
    public LeadDTO update(Long id, LeadDTO leadDetailsDTO) {
        logger.info("Updating lead id: {}", id);
        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lead not found"));

        if (leadDetailsDTO.getAssignTo() != null) {
            User assignedUser = userRepository.getReferenceById(leadDetailsDTO.getAssignTo());
            existingLead.setAssignTo(assignedUser);
        }
        if (leadDetailsDTO.getLeadStatus() != null ) {
            LeadStatus leadStatus = leadStatusRepository.getReferenceById(leadDetailsDTO.getLeadStatus());
            existingLead.setLeadStatus(leadStatus);
        }
        if (leadDetailsDTO.getLeadSource() != null ) {
            LeadSource leadSource = leadSourceRepository.getReferenceById(leadDetailsDTO.getLeadSource());
            existingLead.setLeadSource(leadSource);
        }


        if (leadDetailsDTO.getContactInfo() != null) {
            ContactInfo newContactInfo = contactInfoRepository.findById(leadDetailsDTO.getContactInfo().getId())
                    .orElseThrow(() -> new NoSuchElementException("Contact info not found"));
            existingLead.setContactInfo(newContactInfo);
        } else {
            existingLead.setContactInfo(leadDetailsDTO.getContactInfo());
        }



        Lead leadDetails = leadMapper.leadDTOtoLead(leadDetailsDTO);



        if (leadDetails.getName() != null) {
            existingLead.setName(leadDetails.getName());
        }
        if (leadDetails.getDescription() != null) {
            existingLead.setDescription(leadDetails.getDescription());
        }
        if (leadDetails.getLeadValue() != null) {
            existingLead.setLeadValue(leadDetails.getLeadValue());
        }
        if (leadDetails.getLeadRejectedReasonComment() != null) {
            existingLead.setLeadRejectedReasonComment(leadDetails.getLeadRejectedReasonComment());
        }
        if (leadDetails.getRoomsQuantity() != null) {
            existingLead.setRoomsQuantity(leadDetails.getRoomsQuantity());
        }
        if (leadDetails.getExecutionDate() != null) {
            existingLead.setExecutionDate(leadDetails.getExecutionDate());
        }

        Lead updatedLead = leadRepository.save(existingLead);
        return leadMapper.leadToLeadDTO(updatedLead);
    }

    @Transactional
    public void deleteLead(List<Long> ids) {

        for (Long id : ids) {
            Lead lead = leadRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Lead not found"));
            lead.setContactInfo(null); // Odłącz powiązanie z ContactInfo
            lead.setAssignTo(null); // Odłącz powiązanie z ContactInfo
            lead.setLeadStatus(null);
            lead.setLeadSource(null);

            leadRepository.save(lead); // Zapisz zmiany, aby zaktualizować bazę danych
        }

        leadRepository.deleteAllById(ids);
    }
}
