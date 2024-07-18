package wh.plus.crm.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.LeadDTO;
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
    private final EntityManager entityManager; // Dodane
    private final ContactInfoRepository contactInfoRepository;
    private final LeadMapper leadMapper;

    public List<LeadDTO> findAll() {
       return leadRepository.findAll().stream().map(leadMapper::leadToLeadDTO).collect(Collectors.toList());
    }

    public Optional<LeadDTO> findById(Long id) {
        return leadRepository.findById(id)
                .map(leadMapper::leadToLeadDTO);
    }

    @Transactional
    public LeadDTO save(LeadDTO leadDTO, String username) {
        Lead lead = leadMapper.leadDTOtoLead(leadDTO);
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            lead.setCreatedBy(user.get());
            logger.info("User found: {}", user.get().getUsername());
            Lead savedLead = leadRepository.save(lead);
            logger.info("Lead saved with id: {}", savedLead.getId());
            return leadMapper.leadToLeadDTO(savedLead);
        } else {
            logger.error("User not found: {}", username);
            throw new NoSuchElementException("User not found");
        }
    }

    @Transactional
    public LeadDTO update(Long id, LeadDTO leadDetailsDTO) {
        logger.info("Updating lead id: {}", id);
        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lead not found"));

        Lead leadDetails = leadMapper.leadDTOtoLead(leadDetailsDTO);
        updateLeadProperties(existingLead, leadDetails);

        Lead updatedLead = leadRepository.save(existingLead);
        return leadMapper.leadToLeadDTO(updatedLead);
    }

    private void updateLeadProperties(Lead existingLead, Lead leadDetails) {
        logger.info("Starting updateLeadProperties");

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
        if (leadDetails.getLeadStatus() != null) {
            LeadStatus leadStatus = leadStatusRepository.findById(leadDetails.getLeadStatus().getId())
                    .orElseThrow(() -> new NoSuchElementException("LeadStatus not found"));
            existingLead.setLeadStatus(leadStatus);
        }
        if (leadDetails.getLeadSource() != null) {
            LeadSource leadSource = leadSourceRepository.findById(leadDetails.getLeadSource().getId())
                    .orElseThrow(() -> new NoSuchElementException("LeadSource not found"));
            existingLead.setLeadSource(leadSource);
        }
        if (leadDetails.getContactInfo() != null && leadDetails.getContactInfo().getId() != null) {
            ContactInfo newContactInfo = contactInfoRepository.findById(leadDetails.getContactInfo().getId())
                    .orElseThrow(() -> new NoSuchElementException("ContactInfo not found"));
            existingLead.setContactInfo(newContactInfo);
        }
        if (leadDetails.getAssignTo() != null && leadDetails.getAssignTo().getId() != null) {
            User assignedUser = userRepository.findById(leadDetails.getAssignTo().getId())
                    .orElseThrow(() -> new NoSuchElementException("User not found"));
            existingLead.setAssignTo(assignedUser);
        }

        logger.info("Finished updateLeadProperties");
    }




}
