package wh.plus.crm.service;

import java.util.Arrays;
import java.util.stream.Stream;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import wh.plus.crm.dto.offer.OfferDTO;
import wh.plus.crm.dto.offer.OfferItemDTO;
import wh.plus.crm.helper.NullPropertyUtils;
import wh.plus.crm.mapper.OfferMapper;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.model.user.SalesTeam;
import wh.plus.crm.model.user.User;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.offer.*;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.*;
import wh.plus.crm.specyfications.OfferSpecification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@AllArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferMapper offerMapper;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final SalesTeamRepository salesTeamRepository;
    private final LeadStatusRepository leadStatusRepository;

    public Page<OfferDTO> getOffers(Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        return offerRepository.findAll(pageable).map(offerMapper::toOfferDTO);

    }

    public OfferDTO getOfferById(Long id){
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        return offerMapper.toOfferDTO(offer);
    }

    public Page<OfferDTO> searchOffers(
            String createdBy, String name, ClientType clientType, InvestorType investorType,
            OfferStatus offerStatus, ObjectType objectType, String description, Long userId,
            Long clientId, Long leadId, Long projectId, Long salesTeamId, LocalDateTime startDate,
            LocalDateTime endDate, SalesOpportunityLevel salesOpportunityLevel, String archivedFilter, Pageable pageable) {

        Specification<Offer> specification = Specification.where(null);

        if (createdBy != null) {
            specification = specification.and(OfferSpecification.hasCreatedBy(createdBy));
        }
        if (name != null) {
            specification = specification.and(OfferSpecification.hasName(name));
        }
        if (clientType != null) {
            specification = specification.and(OfferSpecification.hasClientType(clientType));
        }
        if (investorType != null) {
            specification = specification.and(OfferSpecification.hasInvestorType(investorType));
        }
        if (offerStatus != null) {
            specification = specification.and(OfferSpecification.hasOfferStatus(offerStatus));
        }
        if (objectType != null) {
            specification = specification.and(OfferSpecification.hasObjectType(objectType));
        }
        if (description != null) {
            specification = specification.and(OfferSpecification.hasDescription(description));
        }
        if (userId != null) {
            specification = specification.and(OfferSpecification.hasUser(userId));
        }
        if (clientId != null) {
            specification = specification.and(OfferSpecification.hasClient(clientId));
        }
        if (leadId != null) {
            specification = specification.and(OfferSpecification.hasLead(leadId));
        }
        if (projectId != null) {
            specification = specification.and(OfferSpecification.hasProject(projectId));
        }
        if (salesTeamId != null) {
            specification = specification.and(OfferSpecification.hasSalesTeam(salesTeamId));
        }
        if (salesOpportunityLevel != null) {
            specification = specification.and(OfferSpecification.hasSalesOpportunityLevel(salesOpportunityLevel));
        }
        if (startDate != null && endDate != null) {
            specification = specification.and(OfferSpecification.createdBetween(startDate, endDate));
        }

        if (archivedFilter != null) {
            specification = specification.and(OfferSpecification.hasArchivedFilter(archivedFilter));
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return offerRepository.findAll(specification, sortedPageable).map(offerMapper::toOfferDTO);
    }


    @Transactional
    public OfferDTO createOffer(OfferDTO offerDTO) {
        String clientGlobalId = null;

        // Pobierz clientGlobalId z Lead lub Client
        if (offerDTO.getLead() != null && offerDTO.getLead().getId() != null) {
            Lead lead = leadRepository.findById(offerDTO.getLead().getId())
                    .orElseThrow(() -> new NoSuchElementException("Lead not found"));
            clientGlobalId = lead.getClientGlobalId();
        } else if (offerDTO.getClient() != null && offerDTO.getClient().getId() != null) {
            Client client = clientRepository.findById(offerDTO.getClient().getId())
                    .orElseThrow(() -> new NoSuchElementException("Client not found"));
            clientGlobalId = client.getClientGlobalId();
        } else {
            throw new IllegalArgumentException("Offer must be linked to either a Lead or a Client");
        }

        // Mapowanie DTO na encję
        Offer offer = offerMapper.toEntity(offerDTO);

        // Walidacja dla waluty EUR
        if (offer.getCurrency() == Currency.EUR && offerDTO.getEuroExchangeRate() == null) {
            throw new IllegalArgumentException("Brak kursu wymiany dla waluty EUR.");
        }

        // Ustawienie euro exchange rate
        offer.setEuroExchangeRate(offerDTO.getEuroExchangeRate());

        // Ustawienia relacji opcjonalnych
        if (offerDTO.getUser() != null) {
            User user = userRepository.findById(offerDTO.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            offer.setUser(user);
        }

        if (offerDTO.getLead() != null && offerDTO.getLead().getId() != null) {
            Lead lead = leadRepository.findById(offerDTO.getLead().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
            offer.setLead(lead);
        }

        if (offerDTO.getClient() != null && offerDTO.getClient().getId() != null) {
            Client client = clientRepository.findById(offerDTO.getClient().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            offer.setClient(client);
        }

        if (offerDTO.getProject() != null && offerDTO.getProject().getId() != null) {
            Project project = projectRepository.findById(offerDTO.getProject().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            offer.setProject(project);
        }

        if (offerDTO.getSalesTeamId() != null) {
            SalesTeam salesTeam = salesTeamRepository.findById(offerDTO.getSalesTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales Team not found"));
            offer.setSalesTeam(salesTeam);
        }

        // Ustawienie clientGlobalId
        offer.setClientGlobalId(clientGlobalId);

        // Przeliczenie ceny
        recalculateTotalOfferItemsPrice(offer);

        calculateGrossAndTaxForItems(offer);

        // Automatyczne ustawienie isConverted
        offer.setConverted(
                offer.getProject() != null || offer.getSignedContractDate() != null
        );

        // Zapis do bazy
        Offer savedOffer = offerRepository.save(offer);

        // Mapowanie encji na DTO i zwrócenie wyniku
        return offerMapper.toOfferDTO(savedOffer);
    }


    public OfferDTO updateOffer(Long offerId, OfferDTO offerDTO){

        Offer existingOffer = offerRepository.findById(offerId).orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        if (offerDTO.getUser() != null) {
            User user = userRepository.findById(offerDTO.getUser().getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
            existingOffer.setUser(user);
        }

        if (offerDTO.getApprovalReason() == null) {
            existingOffer.setApprovalReason(null);
        }
        if (offerDTO.getRejectionReason() == null) {
            existingOffer.setRejectionReason(null);
        }
        if (offerDTO.getRejectionReasonComment() == null) {
            existingOffer.setRejectionReasonComment(null);
        }
        if (offerDTO.getRejectionOrApprovalDate() == null) {
            existingOffer.setRejectionOrApprovalDate(null);
        }
        if (offerDTO.getSalesOpportunityLevel() == null) {
            existingOffer.setSalesOpportunityLevel(null);
        }

        if (Boolean.TRUE.equals(offerDTO.getRejectLinkedLead())
                && offerDTO.getLead() != null
                && offerDTO.getRejectionReason() != null) {

            Lead lead = leadRepository.findById(offerDTO.getLead().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found"));

            LeadStatus rejectedStatus = leadStatusRepository.findById(7L)
                    .orElseThrow(() -> new IllegalArgumentException("Lead status REJECT not found"));

            lead.setLeadStatus(rejectedStatus);
            lead.setRejectionReason(offerDTO.getRejectionReason());
            lead.setRejectionReasonComment(offerDTO.getRejectionReasonComment());
            lead.setFinal(true);
            leadRepository.save(lead);
        }

        if (offerDTO.getOfferItems() != null){
            for (OfferItemDTO itemDTO : offerDTO.getOfferItems()) {
                 if (itemDTO.getId() != null){
                     boolean belongToOffer = existingOffer.getOfferItemList().stream()
                             .anyMatch(existingItem -> existingItem.getId().equals(itemDTO.getId()));

                     if (!belongToOffer){
                         throw new IllegalArgumentException("OfferItem with ID " + itemDTO.getId() + " does not belong to Offer ID " + offerId);
                     }
                 }
            }
        }

        if (offerDTO.getClient() != null){
            Client client = clientRepository.findById(offerDTO.getClient().getId()).orElseThrow(() -> new IllegalArgumentException("Client not found"));
            existingOffer.setClient(client);
        }

        if (offerDTO.getLead() != null) {
            Lead lead = leadRepository.findById(offerDTO.getLead().getId()).orElseThrow(() -> new IllegalArgumentException("Lead not found"));
            existingOffer.setLead(lead);
        }

        if (offerDTO.getProject() != null) {
            Project project = projectRepository.findById(offerDTO.getProject().getId()).orElseThrow(() -> new IllegalArgumentException("Project not found"));
            existingOffer.setProject(project);
        }

        OfferStatus previousStatus = existingOffer.getOfferStatus();

        // Walidacja przejść statusów
        if (offerDTO.getOfferStatus() != null && !offerDTO.getOfferStatus().equals(previousStatus)) {
            validateStatusTransition(previousStatus, offerDTO.getOfferStatus());
        }

        String[] ignoreProperties = Stream.concat(
                Arrays.stream(NullPropertyUtils.getNullPropertyNames(offerDTO)),
                Stream.of("offerItems", "client", "lead", "project", "user")
        ).toArray(String[]::new);
        BeanUtils.copyProperties(offerDTO, existingOffer, ignoreProperties);
        if (offerDTO.getOfferItems() != null) {

            for (OfferItemDTO itemDTO : offerDTO.getOfferItems()) {
                if (itemDTO.getId() != null) {
                    Optional<OfferItem> existingItemOptional = existingOffer.getOfferItemList().stream()
                            .filter(item -> item.getId().equals(itemDTO.getId())).findFirst();

                    if (existingItemOptional.isPresent()) {
                        OfferItem existingItem = existingItemOptional.get();
                        BeanUtils.copyProperties(itemDTO, existingItem, NullPropertyUtils.getNullPropertyNames(itemDTO));
                    } else {
                        OfferItem newItem = offerMapper.toEntity(itemDTO);
                        newItem.setOffer(existingOffer);
                        existingOffer.getOfferItemList().add(newItem);
                    }
                } else {
                    OfferItem newItem = offerMapper.toEntity(itemDTO);
                    newItem.setOffer(existingOffer);
                    existingOffer.getOfferItemList().add(newItem);
                }
            }

            existingOffer.getOfferItemList().removeIf(item ->
                    item.getId() != null &&
                    offerDTO.getOfferItems().stream()
                            .filter(dto -> dto.getId() != null)
                            .noneMatch(dto -> dto.getId().equals(item.getId()))
                    );

        }


        calculateGrossAndTaxForItems(existingOffer);
        recalculateTotalOfferItemsPrice(existingOffer);

        if (offerDTO.getOfferStatus() != null && !offerDTO.getOfferStatus().equals(previousStatus)) {
            existingOffer.setStatusChangeDate(LocalDateTime.now());
        }

        if (OfferStatus.REJECTED.equals(offerDTO.getOfferStatus()) || OfferStatus.ACCEPTED.equals(offerDTO.getOfferStatus())) {
            if (offerDTO.getRejectionOrApprovalDate() != null) {
                existingOffer.setRejectionOrApprovalDate(offerDTO.getRejectionOrApprovalDate());
            } else {
                existingOffer.setRejectionOrApprovalDate(LocalDateTime.now());
            }
        }

        // Automatyczne ustawienie daty podpisania umowy przy zmianie statusu
        if (offerDTO.getOfferStatus() != null && !offerDTO.getOfferStatus().equals(previousStatus)) {
            if (OfferStatus.SIGNED.equals(offerDTO.getOfferStatus())) {
                existingOffer.setContractSigned(true);
                // Jeśli frontend przesłał datę podpisania — użyj jej, w przeciwnym razie ustaw dzisiaj
                if (offerDTO.getSignedContractDate() != null) {
                    existingOffer.setSignedContractDate(offerDTO.getSignedContractDate());
                } else {
                    existingOffer.setSignedContractDate(LocalDateTime.now());
                }
            } else {
                existingOffer.setContractSigned(false);
                existingOffer.setSignedContractDate(null);
            }
        }

        // Reset decyzji — czyszczenie pól przy cofnięciu do DRAFT
        if (OfferStatus.DRAFT.equals(offerDTO.getOfferStatus()) && !OfferStatus.DRAFT.equals(previousStatus)) {
            existingOffer.setRejectionOrApprovalDate(null);
            existingOffer.setApprovalReason(null);
            existingOffer.setRejectionReason(null);
            existingOffer.setRejectionReasonComment(null);
            existingOffer.setSignedContractDate(null);
            existingOffer.setContractSigned(false);
        }

        // Automatyczne ustawienie isConverted
        existingOffer.setConverted(
                existingOffer.getProject() != null || existingOffer.getSignedContractDate() != null
        );

        Offer updatedOffer = offerRepository.save(existingOffer);
        return offerMapper.toOfferDTO(updatedOffer);



    }

    public void deleteOffer(Long offerId){
        offerRepository.deleteById(offerId);
    }

    public void updateOfferStatus(Long id, OfferStatus offerStatus){
        Offer offer = offerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
        validateStatusTransition(offer.getOfferStatus(), offerStatus);
        offer.setOfferStatus(offerStatus);
        offer.setStatusChangeDate(LocalDateTime.now());

        if (OfferStatus.SIGNED.equals(offerStatus)) {
            offer.setContractSigned(true);
            offer.setSignedContractDate(LocalDateTime.now());
        } else {
            offer.setContractSigned(false);
            offer.setSignedContractDate(null);
        }

        offer.setConverted(
                offer.getProject() != null || offer.getSignedContractDate() != null
        );

        offerRepository.save(offer);
    }

    @Transactional
    public OfferDTO resetDecision(Long offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        offer.setOfferStatus(OfferStatus.DRAFT);
        offer.setStatusChangeDate(LocalDateTime.now());
        offer.setRejectionOrApprovalDate(null);
        offer.setApprovalReason(null);
        offer.setRejectionReason(null);
        offer.setRejectionReasonComment(null);
        offer.setSignedContractDate(null);
        offer.setContractSigned(false);
        offer.setConverted(offer.getProject() != null);

        Offer saved = offerRepository.save(offer);
        return offerMapper.toOfferDTO(saved);
    }

    private BigDecimal getEuroExchangeRate() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.nbp.pl/api/exchangerates/rates/A/EUR?format=json";

            // Pobranie odpowiedzi jako ogólna mapa
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                // Wyciągnięcie listy "rates"
                List<Map<String, Object>> rates = (List<Map<String, Object>>) response.get("rates");
                if (rates != null && !rates.isEmpty()) {
                    // Pobranie kursu i zwrócenie jako BigDecimal
                    return new BigDecimal(rates.get(0).get("mid").toString());
                }
            }
        } catch (Exception e) {
            // Obsługa błędów
            System.err.println("Nie udało się pobrać kursu euro: " + e.getMessage());
        }
        return null; // Zwrot null w przypadku problemu
    }


    private void recalculateTotalOfferItemsPrice(Offer offer) {
        if (offer.getCurrency() == Currency.EUR && offer.getEuroExchangeRate() == null) {
            throw new IllegalArgumentException("Brak kursu wymiany dla waluty EUR.");
        }

        BigDecimal totalAmount = offer.getOfferItemList().stream()
                .filter(item -> item.getQuantity() != null && item.getAmount() != null) // Ignorowanie niepełnych danych
                .map(item -> item.getAmount().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (offer.getCurrency() == Currency.EUR) {
            // Zapisanie wartości w EUR
            offer.setTotalPriceInEUR(totalAmount);

            // Przeliczenie na PLN na podstawie kursu przekazanego w DTO
            BigDecimal totalInPLN = totalAmount.multiply(offer.getEuroExchangeRate());
            offer.setTotalPrice(totalInPLN);
        } else {
            // Domyślne przetwarzanie dla PLN
            offer.setTotalPrice(totalAmount);
            offer.setTotalPriceInEUR(null); // Wartość w EUR nie dotyczy PLN
        }
    }

    private void calculateGrossAndTaxForItems(Offer offer) {
        for (OfferItem item : offer.getOfferItemList()) {
            if(item.getAmount() != null && item.getQuantity() != null && item.getTax() != null) {
                BigDecimal netAmount = (item.getAmount().multiply(BigDecimal.valueOf(item.getQuantity())));
                BigDecimal taxRate = BigDecimal.valueOf(item.getTax().getRate());

                BigDecimal taxAmount = netAmount.multiply(taxRate);

                BigDecimal grossAmount = netAmount.add(taxAmount);

                item.setTaxAmount(taxAmount);
                item.setGrossAmount(grossAmount);


            }
        }
    }

    /**
     * Walidacja dozwolonych przejść statusów oferty.
     *
     * Dozwolone przejścia:
     * DRAFT    → SENT, REJECTED
     * SENT     → ACCEPTED, REJECTED, DRAFT
     * ACCEPTED → SIGNED, REJECTED, DRAFT
     * REJECTED → DRAFT
     * SIGNED   → DRAFT (przez reset decyzji)
     *
     * SIGNED wymaga przejścia przez ACCEPTED — nie można przeskoczyć.
     */
    private void validateStatusTransition(OfferStatus from, OfferStatus to) {
        Map<OfferStatus, Set<OfferStatus>> allowedTransitions = Map.of(
                OfferStatus.DRAFT, Set.of(OfferStatus.SENT, OfferStatus.ACCEPTED, OfferStatus.REJECTED),
                OfferStatus.SENT, Set.of(OfferStatus.ACCEPTED, OfferStatus.REJECTED, OfferStatus.DRAFT),
                OfferStatus.ACCEPTED, Set.of(OfferStatus.SIGNED, OfferStatus.REJECTED, OfferStatus.DRAFT),
                OfferStatus.REJECTED, Set.of(OfferStatus.DRAFT),
                OfferStatus.SIGNED, Set.of(OfferStatus.DRAFT)
        );

        Set<OfferStatus> allowed = allowedTransitions.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                    String.format("Niedozwolona zmiana statusu oferty: %s → %s. Dozwolone przejścia z %s: %s",
                            from.getDescription(), to.getDescription(), from.getDescription(),
                            allowed.stream().map(OfferStatus::getDescription).collect(Collectors.joining(", ")))
            );
        }
    }
}
