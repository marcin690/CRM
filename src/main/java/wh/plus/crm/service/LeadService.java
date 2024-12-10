package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.lead.LeadDTO;
import wh.plus.crm.mapper.LeadMapper;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.repository.LeadRepository;
import wh.plus.crm.repository.LeadSourceRepository;
import wh.plus.crm.repository.LeadStatusRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class LeadService {

    private static final Logger logger = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final LeadStatusRepository leadStatusRepository;
    private final LeadMapper leadMapper;
    private final LeadSourceRepository leadSourceRepository;
    private final ClientGlobalIdService clientGlobalIdService;

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
        Lead lead = leadMapper.leadDTOtoLead(leadDTO,leadStatusRepository);

        if(lead.getClientGlobalId() == null || lead.getClientGlobalId().isEmpty()) {
            lead.setClientGlobalId(clientGlobalIdService.generateClientGlobalId());
        }

        Lead savedLead = leadRepository.save(lead);
        return leadMapper.leadToLeadDTO(savedLead);
    }

    @Transactional
    public LeadDTO update(Long id, LeadDTO leadDetailsDTO) {
        logger.info("Updating lead id: {}", id);
        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lead not found"));

        leadMapper.updateLeadFromDto(leadDetailsDTO, existingLead, leadStatusRepository, leadSourceRepository);

        Lead updatedLead = leadRepository.save(existingLead);
        return leadMapper.leadToLeadDTO(updatedLead);
    }

    public Page<LeadDTO> getLeads(Pageable pageable, LocalDateTime fromDate, LocalDateTime toDate, String employee, Long status, String search ){
        logger.debug("Parametry wejściowe - fromDate: {}, toDate: {}, employee: {}, status: {}, search: {}", fromDate, toDate, employee, status, search);

        if(fromDate == null && toDate == null && employee == null && status == null && (search == null || search.isEmpty())) {
           Pageable defaultPageable = PageRequest.of(pageable.getPageNumber(), 25, pageable.getSort().and(Sort.by("creationDate")));
           return leadRepository.findAll(defaultPageable).map(leadMapper::leadToLeadDTO);
        }

        Page<Lead> filteredLeads = leadRepository.findLeadsByCriteria(pageable, fromDate, toDate, employee, status, search);
        return filteredLeads.map(leadMapper::leadToLeadDTO);
    }

    @Transactional
    public void deleteLead(List<Long> ids) {
        for (Long id : ids) {
            Lead lead = leadRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Lead not found"));
            lead.setAssignTo(null);
            lead.setLeadStatus(null);
            lead.setLeadSource(null);


            leadRepository.save(lead);
        }

        leadRepository.deleteAllById(ids);
    }
}
