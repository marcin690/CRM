package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.mapstruct.control.MappingControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.repository.LeadRepository;
import wh.plus.crm.repository.LeadSourceRepository;
import wh.plus.crm.repository.LeadStatusRepository;
import wh.plus.crm.repository.UserRepository;
import wh.plus.crm.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Service
@AllArgsConstructor
public class LeadService {

    private static final Logger logger = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final LeadStatusRepository leadStatusRepository;
    private final LeadSourceRepository leadSourceRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
        Lead existingLead = entityManager.find(Lead.class, id);
        if (existingLead == null) {
            throw new NoSuchElementException("Lead not found");
        }

        // Aktualizowanie właściwości leada
        updateLeadProperties(existingLead, leadDetails);

        // Aktualizacja przypisanej osoby
        if (leadDetails.getAssignedTo() != null) {
            User newAssignedUser = entityManager.find(User.class, leadDetails.getAssignedTo().getId());
            if (newAssignedUser == null) {
                throw new NoSuchElementException("Assigned user not found");
            }

            // Odpięcie leada od obecnie przypisanego użytkownika (jeśli jest przypisany)
            if (existingLead.getAssignedTo() != null) {
                existingLead.getAssignedTo().getAssignedLeads().remove(existingLead);
                entityManager.merge(existingLead.getAssignedTo());
            }

            // Przypisanie leada do nowego użytkownika
            newAssignedUser.getAssignedLeads().add(existingLead);
            existingLead.setAssignedTo(newAssignedUser);
            entityManager.merge(newAssignedUser);
        } else {
            if (existingLead.getAssignedTo() != null) {
                existingLead.getAssignedTo().getAssignedLeads().remove(existingLead);
                entityManager.merge(existingLead.getAssignedTo());
                existingLead.setAssignedTo(null);
            }
        }

        return entityManager.merge(existingLead);
    }

    private void updateLeadProperties(Lead existingLead, Lead leadDetails) {
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
            LeadStatus leadStatus = entityManager.find(LeadStatus.class, leadDetails.getLeadStatus().getId());
            if (leadStatus == null) {
                throw new NoSuchElementException("LeadStatus not found");
            }
            existingLead.setLeadStatus(leadStatus);
        }
        if (leadDetails.getLeadSource() != null) {
            LeadSource leadSource = entityManager.find(LeadSource.class, leadDetails.getLeadSource().getId());
            if (leadSource == null) {
                throw new NoSuchElementException("LeadSource not found");
            }
            existingLead.setLeadSource(leadSource);
        }
    }
}
