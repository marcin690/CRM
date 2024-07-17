package wh.plus.crm.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.contactInfo.ContactInfo;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.repository.*;
import wh.plus.crm.model.User;




import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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

    public List<Lead> findAll() {
        logger.info("Fetching all leads");
        return leadRepository.findAll();
    }

    public Optional<Lead> findById(Long id) {
        logger.info("Fetching lead by id: {}", id);
        return leadRepository.findById(id);
    }

    @Transactional
    public Lead save(Lead lead, String username) {
        logger.info("Saving new lead for user: {}", username);
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            lead.setCreatedBy(user.get());
            logger.info("User found: {}", user.get().getUsername());
            Lead savedLead = leadRepository.save(lead);
            logger.info("Lead saved with id: {}", savedLead.getId());
            return savedLead;
        } else {
            logger.error("User not found: {}", username);
            throw new NoSuchElementException("User not found");
        }
    }

    @Transactional
    public Lead update(Long id, Lead leadDetails) {
        logger.info("Updating lead id: {}", id);
        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lead not found"));

        // Detach current state of existingLead from Hibernate session to prevent shared references issues
        entityManager.detach(existingLead);

        // Aktualizowanie właściwości leada
        updateLeadProperties(existingLead, leadDetails);

        // Save updated lead
        Lead updatedLead = leadRepository.save(existingLead);
        logger.info("Updated lead: {}", updatedLead);
        return updatedLead;
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
