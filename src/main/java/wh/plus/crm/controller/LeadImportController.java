package wh.plus.crm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.repository.LeadRepository;
import wh.plus.crm.repository.LeadSourceRepository;
import wh.plus.crm.repository.LeadStatusRepository;
import wh.plus.crm.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/import")
public class LeadImportController {

    private final LeadRepository leadRepository;
    private final LeadSourceRepository leadSourceRepository;
    private final LeadStatusRepository leadStatusRepository;
    private final UserRepository userRepository;

    public LeadImportController(LeadRepository leadRepository,
                                LeadSourceRepository leadSourceRepository,
                                LeadStatusRepository leadStatusRepository,
                                UserRepository userRepository) {
        this.leadRepository = leadRepository;
        this.leadSourceRepository = leadSourceRepository;
        this.leadStatusRepository = leadStatusRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/leads")
    public ResponseEntity<String> importLeads(@RequestBody List<Map<String, Object>> oldLeads) {
        for (Map<String, Object> oldLead : oldLeads) {
            Lead lead = mapOldLeadToLead(oldLead);
            leadRepository.save(lead); // Zapis do bazy danych
        }
        return ResponseEntity.ok("Leads imported successfully!");
    }

    private Lead mapOldLeadToLead(Map<String, Object> oldLead) {
        Lead lead = new Lead();
        lead.setSkipAudit(true);
        // Ustawienie ID leada
        lead.setId(parseLongOrDefault((String) oldLead.get("id"), 0L));

        // Mapowanie pól podstawowych
        lead.setName((String) oldLead.getOrDefault("name", "Unknown Lead"));
        lead.setDescription((String) oldLead.getOrDefault("description", ""));
        lead.setClientCity((String) oldLead.getOrDefault("city", ""));
        lead.setClientState((String) oldLead.getOrDefault("state", ""));
        lead.setClientZip((String) oldLead.getOrDefault("zip", ""));
        lead.setClientEmail((String) oldLead.getOrDefault("email", ""));
        lead.setClientPhone(parseLongOrDefault((String) oldLead.get("phonenumber"), 0L));

        // Mapowanie leadValue jako Double
        Object leadValueObj = oldLead.get("lead_value");
        if (leadValueObj != null) {
            try {
                lead.setLeadValue(Double.parseDouble(leadValueObj.toString()));
            } catch (NumberFormatException e) {
                System.err.println("Nieprawidłowy format lead_value: " + leadValueObj);
                lead.setLeadValue(0.0); // Wartość domyślna w przypadku błędu
            }
        } else {
            System.out.println("lead_value jest null, przypisuję wartość domyślną 0.0");
            lead.setLeadValue(0.0);
        }

        // Mapowanie kraju
        String country = (String) oldLead.get("country");
        lead.setClientCountry("176".equals(country) ? "Polska" : country);
        lead.setClientCountry("59".equals(country) ? "Czechy" : country);
        lead.setClientCountry("83".equals(country) ? "Niemcy" : country);
        lead.setClientCountry("209".equals(country) ? "Hiszpania" : country);
        lead.setClientCountry("76".equals(country) ? "Francja" : country);
        lead.setClientCountry("15".equals(country) ? "Austria" : country);


        // Mapowanie źródła
        String source = (String) oldLead.get("source");
        Long oldSourceId = parseLongOrDefault(source, null);
        Long newSourceId = mapOldSourceToNewSource(oldSourceId);
        lead.setLeadSource(newSourceId != null
                ? leadSourceRepository.findById(newSourceId)
                .orElseThrow(() -> new NoSuchElementException("Lead Source not found: " + newSourceId))
                : null);

        // Mapowanie statusu
        LeadStatus status = findLeadStatus(oldLead);
        lead.setLeadStatus(status != null ? status : leadStatusRepository.findById(1L).orElseThrow());

        // Sprawdzenie warunków dla isFinal
        String junk = (String) oldLead.getOrDefault("junk", "0");
        String lost = (String) oldLead.getOrDefault("lost", "0");
        String statusString = (String) oldLead.getOrDefault("status", "");

        if ("1".equals(statusString)) {
            lead.setFinal(true);
        } else if ("1".equals(junk) || "1".equals(lost)) {
            lead.setFinal(true);
        } else {
            lead.setFinal(false);
        }


        String assignedStr = (String) oldLead.get("assigned");
        Long assignedUserId = parseLongOrDefault(assignedStr, null);

        if (assignedUserId != null) {
            // 2. W encji Lead ustawiamy faktyczny obiekt User z bazy:
            lead.setUser(
                    userRepository
                            .findById(assignedUserId)
                            .orElseThrow(() -> new NoSuchElementException("User not found: " + assignedUserId))
            );
        } else {
            lead.setUser(null);
        }

        // Mapowanie dat
        lead.setCreationDate(parseDateOrDefault((String) oldLead.get("dateadded"), LocalDateTime.now()));
        lead.setLastModifiedDate(parseDateOrDefault((String) oldLead.get("last_status_change"), LocalDateTime.now()));

        return lead;
    }

    private LeadStatus findLeadStatus(Map<String, Object> oldLead) {
        // Zakładamy, że domyślnie (jeśli brak wartości) old system używał "0" (nieodrzucony).
        String junk = (String) oldLead.getOrDefault("junk", "0");
        String lost = (String) oldLead.getOrDefault("lost", "0");

        // Jeśli w starym systemie jest junk=1 lub lost=1, uznajemy lead za odrzucony (status 7 w nowym)
        if ("1".equals(junk) || "1".equals(lost)) {
            return leadStatusRepository.findById(7L) // 7 = Odrzucony
                    .orElseThrow(() -> new NoSuchElementException("Lead Status not found: 7"));
        }

        // Jeśli pole "status" w starym systemie jest puste/nieustawione,
        // to w nowym systemie ustawiamy np. 9 (lub inny domyślny status)
        String statusString = (String) oldLead.get("status");
        if (statusString == null || statusString.isEmpty()) {
            return leadStatusRepository.findById(9L) // np. "Do weryfikacji"
                    .orElseThrow(() -> new NoSuchElementException("Lead Status not found: 9"));
        }

        // W przeciwnym razie – mapujemy stare ID statusu na nowe ID według Twojego switch-a
        Long oldStatusId = parseLongOrDefault(statusString, null);
        Long newStatusId = mapOldStatusToNewStatus(oldStatusId);

        return leadStatusRepository.findById(newStatusId)
                .orElseThrow(() -> new NoSuchElementException("Lead Status not found: " + newStatusId));
    }



    private Long mapOldStatusToNewStatus(Long oldStatusId) {
        if (oldStatusId == null) {
            return 9L; // Zweryfikuj po migracji danych
        }

        switch (oldStatusId.intValue()) {
            case 1: return 5L; // Klient
            case 5: return 1L; // Nowy
            case 8: return 6L; // Czekamy na uzupełnienie dokumentacji
            case 7: return 4L; // Oferta wysłana
            case 10: return 8L; // Kontakt w przyszłości
            case 9: return 2L; // W trakcie weryfikacji
            case 6: return 3L; // W trakcie wyceny
            default: return 9L; // Zweryfikuj po migracji danych
        }
    }


    private Long mapOldSourceToNewSource(Long oldSourceId) {
        if (oldSourceId == null) {
            return null;
        }

        switch (oldSourceId.intValue()) {
            case 14: return 6L;  // Biuro projektowe
            case 2: return 11L;  // Facebook
            case 3: return 1L;   // Formularz kontaktowy
            case 10: return 16L;  // Inne
            case 7: return 3L;   // Klient powracający
            case 11: return 15L; // Kompas
            case 9: return 4L;   // Kontakt telefoniczny
            case 6: return 2L;   // Kontakt własny
            case 5: return 9L;   // Kontakt ze strony
            case 8: return 7L;   // Targi
            case 13: return 15L;  // Zwiadowca
            default: return null;
        }
    }

    private Long parseLongOrDefault(String value, Long defaultValue) {
        try {
            return value != null ? Long.valueOf(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private LocalDateTime parseDateOrDefault(String date, LocalDateTime defaultDate) {
        try {
            return date != null ? LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : defaultDate;
        } catch (Exception e) {
            return defaultDate;
        }
    }
}
