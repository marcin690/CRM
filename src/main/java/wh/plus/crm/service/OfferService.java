package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import wh.plus.crm.dto.offer.OfferDTO;
import wh.plus.crm.dto.offer.OfferItemDTO;
import wh.plus.crm.helper.NullPropertyUtils;
import wh.plus.crm.mapper.OfferMapper;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.User;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.offer.OfferItem;
import wh.plus.crm.model.offer.OfferStatus;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.*;

import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

    public Page<OfferDTO> getOffers(Pageable pageable) {
        return offerRepository.findAll(pageable).map(offerMapper::toOfferDTO);
    }

    public OfferDTO getOfferById(Long id){
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        return offerMapper.toOfferDTO(offer);
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
        if (offerDTO.getUserId() != null) {
            User user = userRepository.findById(offerDTO.getUserId())
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

        // Ustawienie clientGlobalId
        offer.setClientGlobalId(clientGlobalId);

        // Przeliczenie ceny
        recalculateTotalOfferItemsPrice(offer);

        // Zapis do bazy
        Offer savedOffer = offerRepository.save(offer);

        // Mapowanie encji na DTO i zwrócenie wyniku
        return offerMapper.toOfferDTO(savedOffer);
    }


    public OfferDTO updateOffer(Long offerId, OfferDTO offerDTO){

        Offer existingOffer = offerRepository.findById(offerId).orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        if (offerDTO.getUserId() != null) {
            User user = userRepository.findById(offerDTO.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
            existingOffer.setUser(user);
        } else {
            existingOffer.setUser(null);
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
        } else {
            existingOffer.setClient(null);
        }

        if (offerDTO.getLead() != null) {
            Lead lead = leadRepository.findById(offerDTO.getLead().getId()).orElseThrow(() -> new IllegalArgumentException("Lead not found"));
            existingOffer.setLead(lead);
        } else {
            existingOffer.setLead(null);
        }

        if (offerDTO.getProject() != null) {
            Project project = projectRepository.findById(offerDTO.getProject().getId()).orElseThrow(() -> new IllegalArgumentException("Project not found"));
            existingOffer.setProject(project);
        } else  {
            existingOffer.setProject(null);
        }

        BeanUtils.copyProperties(offerDTO, existingOffer, NullPropertyUtils.getNullPropertyNames(offerDTO));
        if (offerDTO.getOfferItems() != null) {

            for (OfferItemDTO itemDTO : offerDTO.getOfferItems()) {
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
            }

            existingOffer.getOfferItemList().removeIf(item ->
                    offerDTO.getOfferItems().stream().noneMatch(dto -> dto.getId().equals(item.getId()))
                    );

        }



        recalculateTotalOfferItemsPrice(existingOffer);

        Offer updatedOffer = offerRepository.save(existingOffer);
        return offerMapper.toOfferDTO(updatedOffer);



    }

    public void deleteOffer(Long offerId){
        offerRepository.deleteById(offerId);
    }

    public void updateOfferStatus(Long id, OfferStatus offerStatus){
        Offer offer = offerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
        offer.setOfferStatus(offerStatus);
        offerRepository.save(offer);
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
                .map(item -> BigDecimal.valueOf(item.getAmount() * item.getQuantity()))
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
}