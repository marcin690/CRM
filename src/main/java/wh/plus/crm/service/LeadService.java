package wh.plus.crm.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.lead.LeadDTO;
import wh.plus.crm.events.EntityCreatedEvent;
import wh.plus.crm.mapper.LeadMapper;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.repository.*;
import wh.plus.crm.model.offer.Offer;

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
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ClientRepository clientRepository;
    private final OfferRepository offerRepository;




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
        Lead lead = leadMapper.leadDTOtoLead(leadDTO,leadStatusRepository,leadSourceRepository);

        if(lead.getClientGlobalId() == null || lead.getClientGlobalId().isEmpty()) {
            lead.setClientGlobalId(clientGlobalIdService.generateClientGlobalId());
        }

        Lead savedLead = leadRepository.save(lead);
//        applicationEventPublisher.publishEvent(new EntityCreatedEvent<>(this, savedLead, "Utworzono nowy lead"));


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

    public Page<LeadDTO> getLeads(Pageable pageable, LocalDateTime fromDate, LocalDateTime toDate, String employee, Long status, String search) {
        logger.debug("Parametry wejściowe - fromDate: {}, toDate: {}, employee: {}, status: {}, search: {}", fromDate, toDate, employee, status, search);

        // Sprawdzenie, czy pageSize został przekazany jako 0 (co oznacza: pobierz wszystkie)
        if (pageable.getPageSize() == 0) {
            pageable = PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
        }

        // Jeśli brak filtrów, po prostu zwracamy wszystkie rekordy
        if (fromDate == null && toDate == null && employee == null && status == null && (search == null || search.isEmpty())) {
            return leadRepository.findAll(pageable)
                    .map(leadMapper::leadToLeadDTO);
        }

        // Użycie kryteriów filtrujących
        Page<Lead> filteredLeads = leadRepository.findLeadsByCriteria(pageable, fromDate, toDate, employee, status, search);
        return filteredLeads.map(leadMapper::leadToLeadDTO);
    }

    @Transactional
    public void deleteLead(List<Long> ids) {
        for (Long id : ids) {
            Lead lead = leadRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Lead not found"));

            if (lead.getOffers() != null && !lead.getOffers().isEmpty()) {
                for (Offer offer : lead.getOffers()) {
                    offer.setLead(null); // Odłączenie leadu od oferty
                    offerRepository.save(offer); // Zapis zmian w bazie danych
                }
            }



            lead.setUser(null);
            lead.setLeadStatus(null);
            lead.setLeadSource(null);


            leadRepository.save(lead);
        }

        leadRepository.deleteAllById(ids);
    }

    @Transactional
    public Client convertLeadToClient(Long leadId) {
        // Pobranie leada z bazy
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with ID: " + leadId));

        // Tworzymy nowego klienta, kopiując dane z leada
        Client client = new Client();
        client.setClientBusinessName(lead.getClientBusinessName());
        client.setClientAdress(lead.getClientAdress());
        client.setClientCity(lead.getClientCity());
        client.setClientState(lead.getClientState());
        client.setClientZip(lead.getClientZip());
        client.setClientCountry(lead.getClientCountry());
        client.setClientEmail(lead.getClientEmail());
        client.setClientPhone(lead.getClientPhone());
        client.setVatNumber(lead.getVatNumber());
        client.setClientNotes("Converted from Lead ID: " + leadId);
        // Ustaw globalne ID z leada
        client.setClientGlobalId(lead.getClientGlobalId());
        clientRepository.save(client);

        // Przypisanie ofert z leada do klienta
        if (lead.getOffers() != null && !lead.getOffers().isEmpty()) {
            for (Offer offer : lead.getOffers()) {
                // Ustawiamy nowego klienta dla oferty
                offer.setClient(client);
                // Opcjonalnie: zerujemy powiązanie z leadem, jeśli już nie jest potrzebne
                offer.setLead(null);
                offerRepository.save(offer);
            }
        }

        // Aktualizacja statusu leada
        LeadStatus leadStatus = leadStatusRepository.findByStatusName("CLIENT")
                .orElseThrow(() -> new IllegalArgumentException("LeadStatus 'CLIENT' not found"));

        lead.setFinal(true);
        lead.setLeadStatus(leadStatus);
        leadRepository.save(lead);

        return client;
    }


}
