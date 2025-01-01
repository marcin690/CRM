package wh.plus.crm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.model.Tax;
import wh.plus.crm.model.offer.*;
import wh.plus.crm.model.RejectionReason;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/import")
public class OfferImportController {

    private static final Logger logger = LoggerFactory.getLogger(OfferImportController.class);

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;

    public OfferImportController(OfferRepository offerRepository,
                                 UserRepository userRepository,
                                 LeadRepository leadRepository,
                                 ClientRepository clientRepository) {
        this.offerRepository = offerRepository;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.clientRepository = clientRepository;
    }

    @PostMapping("/offers")
    public ResponseEntity<Map<String, Object>> importOffers(@RequestBody List<Map<String, Object>> oldOffers) {
        List<Map<String, Object>> skippedOffers = new ArrayList<>();
        List<Map<String, Object>> importedOffers = new ArrayList<>();

        for (Map<String, Object> oldOffer : oldOffers) {
            String offerIdStr = String.valueOf(oldOffer.get("id"));
            Long offerId = parseLongOrDefault(offerIdStr, null);

            if (offerId == null) {
                Map<String, Object> skippedOfferInfo = new HashMap<>();
                skippedOfferInfo.put("id", offerIdStr);
                skippedOfferInfo.put("reason", "Invalid or missing 'id'");
                skippedOffers.add(skippedOfferInfo);
                logger.warn("Skipping offer with invalid or missing 'id': {}", offerIdStr);
                continue;
            }

            // Sprawdzenie, czy oferta już istnieje
            if (offerRepository.existsById(offerId)) {
                Map<String, Object> skippedOfferInfo = new HashMap<>();
                skippedOfferInfo.put("id", offerIdStr);
                skippedOfferInfo.put("reason", "Offer with this ID already exists");
                skippedOffers.add(skippedOfferInfo);
                logger.warn("Skipping offer with existing 'id': {}", offerIdStr);
                continue;
            }

            try {
                // Mapowanie starego obiektu -> Offer
                Offer offer = mapOldOfferToOffer(oldOffer);

                // Zapis do bazy
                offerRepository.save(offer);

                // Dodanie do listy zaimportowanych ofert (opcjonalnie)
                Map<String, Object> importedOfferInfo = new HashMap<>();
                importedOfferInfo.put("id", offer.getId());
                importedOffers.add(importedOfferInfo);
            } catch (Exception e) {
                // Dodanie oferty do listy pominiętych wraz z przyczyną
                Map<String, Object> skippedOfferInfo = new HashMap<>();
                skippedOfferInfo.put("id", offerIdStr);
                skippedOfferInfo.put("reason", e.getMessage());
                skippedOffers.add(skippedOfferInfo);

                // Logowanie błędu
                logger.error("Błąd podczas importu oferty o ID: {} - {}", offerIdStr, e.getMessage());
            }
        }

        // Przygotowanie odpowiedzi z listą zaimportowanych i pominiętych ofert
        Map<String, Object> response = new HashMap<>();
        response.put("importedOffers", importedOffers);
        response.put("skippedOffers", skippedOffers);

        return ResponseEntity.ok(response);
    }

    /**
     * Główna metoda mapująca pola starego JSON-a na nowy obiekt Offer.
     */
    private Offer mapOldOfferToOffer(Map<String, Object> oldOffer) {
        Offer offer = new Offer();

        // 1. Ustawiamy ID takie jak w starym systemie
        Long oldId = parseLongOrDefault(String.valueOf(oldOffer.get("id")), null);
        if (oldId != null) {
            offer.setId(oldId);
        } else {
            throw new IllegalArgumentException("Invalid 'id' for offer");
        }

        // 2. subject -> name
        offer.setName((String) oldOffer.getOrDefault("subject", "Brak nazwy"));

        // 3. datecreated -> creationDate
        String dateCreatedStr = (String) oldOffer.get("datecreated");
        offer.setCreationDate(parseDate(dateCreatedStr));

        // 4. subtotal -> totalPrice
        String subtotalStr = String.valueOf(oldOffer.getOrDefault("subtotal", "0.00"));
        offer.setTotalPrice(parseBigDecimal(subtotalStr));

        // 5. Zawsze ustawiamy walutę na PLN
        offer.setCurrency(Currency.PLN);

        // 6. Mapowanie statusu
        String oldStatus = String.valueOf(oldOffer.get("status"));
        OfferStatus offerStatus = mapOldStatusToNewOfferStatus(oldStatus);
        offer.setOfferStatus(offerStatus);

        // 6a. Jeśli status to ACCEPTED lub REJECTED, ustaw rejectionOrApprovalDate na 31.12.2024
        if (offerStatus == OfferStatus.ACCEPTED || offerStatus == OfferStatus.REJECTED) {
            offer.setRejectionOrApprovalDate(LocalDateTime.of(2024, 12, 31, 0, 0));
        }

        // 7. rel_type + rel_id -> lead lub client
        String relType = (String) oldOffer.get("rel_type");
        String relIdStr = String.valueOf(oldOffer.get("rel_id"));
        Long relId = parseLongOrDefault(relIdStr, null);

        if ("lead".equalsIgnoreCase(relType) && relId != null) {
            Lead lead = leadRepository.findById(relId)
                    .orElse(null);
            if (lead != null) {
                offer.setLead(lead);
                offer.setClientGlobalId(lead.getClientGlobalId());
            } else {
                throw new NoSuchElementException("Lead not found: " + relId);
            }
        } else if ("customer".equalsIgnoreCase(relType) && relId != null) {
            Client client = clientRepository.findById(relId)
                    .orElse(null);
            if (client != null) {
                offer.setClient(client);
                offer.setClientGlobalId(client.getClientGlobalId());
            } else {
                throw new NoSuchElementException("Client not found: " + relId);
            }
        } else {
            throw new IllegalArgumentException("Invalid rel_type or rel_id: " + relType + ", " + relIdStr);
        }

        // 8. Domyślne wartości pól
        offer.setArchived(false);
        offer.setContractSigned(false);

        // 9. customfields -> mapowanie na poszczególne pola w Offer
        List<Map<String, Object>> customFields = (List<Map<String, Object>>) oldOffer.get("customfields");
        if (customFields != null) {
            for (Map<String, Object> field : customFields) {
                String label = (String) field.get("label");
                String value = (String) field.get("value");

                if ("Typ klienta".equalsIgnoreCase(label)) {
                    offer.setClientType(mapClientType(value));
                }

                if ("Poziom szansy sprzedaży".equalsIgnoreCase(label)) {
                    offer.setSalesOpportunityLevel(mapSalesOpportunityLevel(value));
                }

                if ("Handlowiec".equalsIgnoreCase(label)) {
                    Long userId = mapUserNameToId(value);
                    if (userId != null) {
                        User user = userRepository.findById(userId)
                                .orElse(null);
                        if (user != null) {
                            offer.setUser(user);
                        } else {
                            throw new NoSuchElementException("User not found: " + userId);
                        }
                    }
                }

                if ("Powód orzucenia oferty".equalsIgnoreCase(label)) {
                    RejectionReason reason = mapRejectionReason(value);
                    offer.setRejectionReason(reason);
                }

                if ("Komentarz do odrzucenia".equalsIgnoreCase(label)) {
                    offer.setRejectionReasonComment(value);
                }

                if ("Data podpisania umowy".equalsIgnoreCase(label)) {
                    if (value != null && !value.isEmpty()) {
                        offer.setSignedContractDate(parseDate(value));
                        offer.setContractSigned(true);
                    }
                }

                if ("Typ obiektu".equalsIgnoreCase(label)) {
                    offer.setObjectType(mapObjectType(value));
                }
            }
        }

        // 10. Mappowanie offerItems
        // Pobieramy 'total_tax' i 'total' z oryginalnych danych
        String totalTaxStr = String.valueOf(oldOffer.getOrDefault("total_tax", "0.00"));
        String totalStr = String.valueOf(oldOffer.getOrDefault("total", "0.00"));
        String subTotalStr = String.valueOf(oldOffer.getOrDefault("subtotal", "0.00"));

        BigDecimal totalTax = parseBigDecimal(totalTaxStr);
        BigDecimal total = parseBigDecimal(totalStr);
        BigDecimal subTotal = parseBigDecimal(subTotalStr);

        List<OfferItem> offerItems = new ArrayList<>();
        OfferItem item = new OfferItem();
        // Nie ustawiamy ID ręcznie, zakładamy, że jest automatycznie generowane
        item.setTitle((String) oldOffer.getOrDefault("title", "Pozycja oferty"));
        item.setDescription((String) oldOffer.getOrDefault("description", ""));
        item.setAmount(subTotal);
        item.setQuantity(1L);
        item.setTax(Tax.VAT_23); // Zakładam, że podatek jest stały, można to również mapować
        item.setTaxAmount(totalTax);
        item.setGrossAmount(total);
        item.setOffer(offer); // Powiązanie z ofertą

        offerItems.add(item);
        offer.setOfferItemList(offerItems);

        return offer;
    }

    /**
     * Pomocnicza metoda do mapowania statusu.
     */
    private OfferStatus mapOldStatusToNewOfferStatus(String oldStatus) {
        if (oldStatus == null) {
            return OfferStatus.DRAFT; // domyślnie
        }
        switch (oldStatus) {
            case "6":
                return OfferStatus.DRAFT;
            case "4":
                return OfferStatus.SENT;
            case "3":
                return OfferStatus.ACCEPTED;
            case "2":
                return OfferStatus.REJECTED;
            default:
                return OfferStatus.DRAFT;
        }
    }

    /**
     * Mapowanie etykiety "Typ klienta" -> clientType
     */
    private ClientType mapClientType(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "klient nowy":
                return ClientType.NEW_CLIENT;
            case "klient powracający":
                return ClientType.RETURNING_CLIENT;
            case "klient obecnie obsługiwany":
                return ClientType.CURRENT_CLIENT;
            default:
                return null;
        }
    }

    /**
     * Mapowanie etykiety "Poziom szansy sprzedaży" -> SalesOpportunityLevel
     */
    private SalesOpportunityLevel mapSalesOpportunityLevel(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "mały":
                return SalesOpportunityLevel.LOW;
            case "umiarkowany":
                return SalesOpportunityLevel.MODERATE;
            case "duży":
                return SalesOpportunityLevel.HIGH;
            default:
                return null;
        }
    }

    /**
     * Mapowanie etykiety "Handlowiec" -> user_id
     * (dalej w kodzie ściągamy go z bazy przez userRepository.findById(...)).
     */
    private Long mapUserNameToId(String salesmanName) {
        if (salesmanName == null) return null;
        switch (salesmanName) {
            case "Grzegorz Raczek":
                return 8L;
            case "Rafał Hyla":
                return 12L;
            case "Dariusz Kwieciński":
                return 7L;
            case "Krystian Kwieciński":
                return 9L;
            case "Joanna Apryjas":
                return 10L;
            case "Jagoda Szymczak":
                return 2L;
            default:
                return null;
        }
    }

    /**
     * Mapowanie etykiety "Powód orzucenia oferty" -> RejectionReason
     */
    private RejectionReason mapRejectionReason(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "za wysoka cena":
                return RejectionReason.PRICE_TOO_HIGH;
            case "ustawiony przetarg":
                return RejectionReason.RIGGED_TENDER;
            case "inne":
                return RejectionReason.OTHER;
            default:
                return null; // Możesz dostosować w razie potrzeby
        }
    }

    /**
     * Mapowanie etykiety "Typ obiektu" -> ObjectType
     */
    private ObjectType mapObjectType(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "hotele duże":
                return ObjectType.LARGE_HOTELS;
            case "hotele sieciowe":
                return ObjectType.CHAIN_HOTELS;
            case "condohotele":
                return ObjectType.CONDO_HOTELS;
            case "apartamenty":
                return ObjectType.APARTMENTS;
            case "hotele niezależne":
                return ObjectType.INDEPENDENT_HOTELS;
            case "sklep":
                return ObjectType.STORE;
            default:
                return null;
        }
    }

    /**
     * Metoda pomocnicza do parsowania Long.
     */
    private Long parseLongOrDefault(String value, Long defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Metoda pomocnicza do parsowania daty (yyyy-MM-dd HH:mm:ss lub yyyy-MM-dd).
     * Jeśli nie da się sparsować – rzuca wyjątek.
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            throw new IllegalArgumentException("Date string is null or empty");
        }
        // Spróbuj najpierw wzorca z godziną
        try {
            DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateStr, formatterFull);
        } catch (Exception e) {
            // Jak się nie uda – spróbuj tylko do "yyyy-MM-dd"
            try {
                DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return LocalDateTime.parse(dateStr + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                throw new IllegalArgumentException("Invalid date format: " + dateStr);
            }
        }
    }

    /**
     * Metoda pomocnicza do parsowania BigDecimal.
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            // Zamiana przecinka na kropkę, jeśli występuje
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
