package wh.plus.crm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.repository.ClientRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/import")
public class ClientImportController {

    private final ClientRepository clientRepository;

    public ClientImportController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @PostMapping("/clients")
    public ResponseEntity<String> importClients(@RequestBody List<Map<String, Object>> clientsJson) {
        for (Map<String, Object> clientData : clientsJson) {
            Client client = mapJsonToClient(clientData);
            clientRepository.save(client); // Zapisz klienta w bazie
        }
        return ResponseEntity.ok("Clients imported successfully!");
    }

    private Client mapJsonToClient(Map<String, Object> clientData) {
        Client client = new Client();

        // Ustawienie ID na podstawie `userid`
        client.setId(parseLongOrDefault((String) clientData.get("userid"), null));

        // Mapowanie pól podstawowych
        client.setClientFullName((String) clientData.get("company"));
        client.setClientBusinessName((String) clientData.get("company"));
        client.setClientAdress((String) clientData.get("address"));
        client.setClientCity((String) clientData.get("city"));
        client.setClientState((String) clientData.get("state"));
        client.setClientZip((String) clientData.get("zip"));
        client.setClientCountry(mapCountryCodeToName((String) clientData.get("country")));
        client.setClientEmail(""); // Brak pola email w JSON
        client.setClientPhone(parseLongOrDefault((String) clientData.get("phonenumber"), null));
        client.setVatNumber(parseLongOrDefault((String) clientData.get("vat"), null));
        client.setClientNotes(""); // Brak notatek w JSON

        // Domyślne wartości dla dodatkowych pól
        client.setVersion(0);
        client.setFakturowniaCategory(""); // Pole customowe, puste

        // Mapowanie daty utworzenia
        String dateCreated = (String) clientData.get("datecreated");
        client.setCreationDate(parseDateOrDefault(dateCreated, LocalDateTime.now()));
        client.setLastModifiedDate(LocalDateTime.now());

        // Relacje - pominięte (contacts, events, offers, projects)
        client.setContacts(null);
        client.setEvents(null);
        client.setOffers(null);
        client.setProjects(null);

        return client;
    }

    private Long parseLongOrDefault(String value, Long defaultValue) {
        try {
            return value != null && !value.isEmpty() ? Long.valueOf(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private LocalDateTime parseDateOrDefault(String date, LocalDateTime defaultDate) {
        try {
            return date != null && !date.isEmpty() ?
                    LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                    defaultDate;
        } catch (Exception e) {
            return defaultDate;
        }
    }

    private String mapCountryCodeToName(String code) {
        if ("176".equals(code)) {
            return "Polska";
        }
        // Dodaj inne mapowania, jeśli potrzebne
        return "Unknown";
    }
}
