package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.LeadDTO;
import wh.plus.crm.mapper.LeadMapper;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.repository.LeadRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LeadService {

    private static final Logger logger = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final LeadMapper leadMapper;

    @Transactional
    public List<LeadDTO> findAll() {
        return leadRepository.findAll().stream()
                .map(leadMapper::leadToLeadDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<LeadDTO> findById(Long id) {
        return leadRepository.findById(id)
                .map(leadMapper::leadToLeadDTO);
    }


    @Transactional
    public LeadDTO save(LeadDTO leadDTO) {
        Lead lead = leadMapper.leadDTOtoLead(leadDTO);
        Lead savedLead = leadRepository.save(lead);
        return leadMapper.leadToLeadDTO(savedLead);
    }

    @Transactional
    public LeadDTO update(Long id, LeadDTO leadDetailsDTO) {
        logger.info("Updating lead id: {}", id);
        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lead not found"));

        leadMapper.updateLeadFromDto(leadDetailsDTO, existingLead);
        Lead updatedLead = leadRepository.save(existingLead);
        return leadMapper.leadToLeadDTO(updatedLead);
    }

    @Transactional
    public void deleteLead(List<Long> ids) {
        for (Long id : ids) {
            Lead lead = leadRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Lead not found"));
            lead.setContactInfo(null);
            lead.setAssignTo(null);
            lead.setLeadStatus(null);
            lead.setLeadSource(null);

            leadRepository.save(lead);
        }

        leadRepository.deleteAllById(ids);
    }
}
