package wh.plus.crm.controller;

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
    public ResponseEntity<String> importOffers(@RequestBody List<Map<String, Object>> oldOffers) {
        for (Map<String, Object> oldOffer : oldOffers) {
            try {
                // Mapowanie starego obiektu -> Offer
                Offer offer = mapOldOfferToOffer(oldOffer);

                // Zapis do bazy
                offerRepository.save(offer);
            } catch (Exception e) {
                // Logowanie błędu i kontynuacja importu następnych ofert
                // Możesz użyć loggera zamiast System.err
                System.err.println("Błąd podczas importu oferty o ID: " + oldOffer.get("id") + " - " + e.getMessage());
            }
        }

        return ResponseEntity.ok("Offers imported successfully!");
    }

    /**
     * Główna metoda mapująca pola starego JSON-a na nowy obiekt Offer.
     */
    private Offer mapOldOfferToOffer(Map<String, Object> oldOffer) {
        Offer offer = new Offer();

        // 1. Ustawiamy ID takie jak w starym systemie
        //    Uwaga: jeśli mamy już oferty w bazie, to zależnie od strategii
        //    trzeba ewentualnie sprawdzić, czy nie występuje konflikt kluczy.
        Long oldId = parseLongOrDefault((String) oldOffer.get("id"), null);
        if (oldId != null) {
            offer.setId(oldId);
        }

        // 2. subject -> name
        offer.setName((String) oldOffer.getOrDefault("subject", "Brak nazwy"));

        // 3. datecreated -> creationDate
        //    Format w starym systemie może wyglądać na "yyyy-MM-dd HH:mm:ss".
        //    Dla bezpieczeństwa spróbujmy sparsować i w razie błędu ustawić LocalDateTime.now().
        String dateCreatedStr = (String) oldOffer.get("datecreated");
        offer.setCreationDate(parseDate(dateCreatedStr));

        // 4. subtotal -> totalPrice
        //    To jest pole w BigDecimal w nowym systemie.
        String subtotalStr = (String) oldOffer.getOrDefault("subtotal", "0.00");
        offer.setTotalPrice(parseBigDecimal(subtotalStr));

        // 5. Zawsze ustawiamy walutę na PLN
        offer.setCurrency(Currency.PLN);

        // 6. Mapowanie statusu (z pola "status" w starym JSON) -> OfferStatus
        String oldStatus = (String) oldOffer.get("status");
        OfferStatus offerStatus = mapOldStatusToNewOfferStatus(oldStatus);
        offer.setOfferStatus(offerStatus);

        // 6a. Jeśli status to ACCEPTED lub REJECTED, ustaw rejectionOrApprovalDate na 31.12.2024
        if (offerStatus == OfferStatus.ACCEPTED || offerStatus == OfferStatus.REJECTED) {
            offer.setRejectionOrApprovalDate(LocalDateTime.of(2024, 12, 31, 0, 0));
        }

        // 7. rel_type + rel_id -> lead lub client
        String relType = (String) oldOffer.get("rel_type");
        String relIdStr = (String) oldOffer.get("rel_id");
        Long relId = parseLongOrDefault(relIdStr, null);

        if ("lead".equalsIgnoreCase(relType) && relId != null) {
            // 7a. Znajdź Lead o danym ID i przypisz do oferty
            Lead lead = leadRepository.findById(relId)
                    .orElseThrow(() -> new NoSuchElementException("Lead not found: " + relId));
            offer.setLead(lead);
        } else if ("customer".equalsIgnoreCase(relType) && relId != null) {
            // 7b. Znajdź Client o danym ID i przypisz do oferty
            Client client = clientRepository.findById(relId)
                    .orElseThrow(() -> new NoSuchElementException("Client not found: " + relId));
            offer.setClient(client);
        }

        // 8. Domyślne wartości pól, których nie mamy w starym systemie
        //    (lub mamy, ale chcemy ustawiać na "z góry" narzucone)
        offer.setArchived(false);
        offer.setContractSigned(false);
        // Poniżej można ustawić też np. offer.setDescription(...) jeśli było w "content",
        // ale w zadaniu mamy je pominąć, chyba że chcesz to zmapować.

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
                                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
                        offer.setUser(user);
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
                    // Parsujemy datę - w starym systemie mogła być np. "2024-10-11"
                    // lub puste pole. Jeśli niepuste, ustawiamy w Offer + contractSigned=true
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
        //    Zakładamy, że każda oferta ma jedną pozycję z określonymi wartościami
        List<OfferItem> offerItems = new ArrayList<>();
        OfferItem item = new OfferItem();
        item.setTitle("Pozycja oferty - tutaj zawsze to samo");
        item.setAmount(offer.getTotalPrice());
        item.setQuantity(1L);
        item.setTax(Tax.valueOf("VAT_23"));
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
                return RejectionReason.OTHER; // Możesz dostosować w razie potrzeby
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
     * Jeśli nie da się sparsować – zwracamy LocalDateTime.now().
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
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
                return LocalDateTime.now();
            }
        }
    }

    /**
     * Metoda pomocnicza do parsowania BigDecimal.
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
