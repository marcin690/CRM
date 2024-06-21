package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.repository.LeadRepository;
import wh.plus.crm.repository.UserRepository;
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
        Optional<Lead> existingLead = leadRepository.findById(id);
        if (existingLead.isPresent()) {
            Lead lead = existingLead.get();
            if (leadDetails.getName() != null) {
                logger.info("Updating name to: {}", leadDetails.getName());
                lead.setName(leadDetails.getName());
            }
            if (leadDetails.getLeadValue() != null) {
                logger.info("Updating leadValue to: {}", leadDetails.getLeadValue());
                lead.setLeadValue(leadDetails.getLeadValue());
            }
            return leadRepository.save(lead);
        } else {
            logger.error("Lead not found: {}", id);
            throw new NoSuchElementException("Lead not found");
        }
    }
}
