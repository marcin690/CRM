package wh.plus.crm.service.fakturownia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import wh.plus.crm.dto.invoice.FakturowniaAccountDTO;
import wh.plus.crm.mapper.FakturowniaAccountMapper;
import wh.plus.crm.model.invoice.FakturowniaAccount;
import wh.plus.crm.repository.FakturowniaAccountRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FakturowniaAccountService {

    private final FakturowniaAccountRepository repository;
    private final FakturowniaAccountMapper mapper;
    private final FakturowniaClient fakturowniaClient;

    public List<FakturowniaAccountDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public FakturowniaAccountDTO findById(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id)));
    }

    public FakturowniaAccountDTO create(FakturowniaAccountDTO dto) {
        FakturowniaAccount account = mapper.toEntity(dto);
        return mapper.toDto(repository.save(account));
    }

    public FakturowniaAccountDTO update(Long id, FakturowniaAccountDTO dto) {
        FakturowniaAccount account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        // Pusty token = "nie zmieniaj" (admin nie podał ponownie). MapStruct IGNORE
        // łapie tylko null, więc neutralizujemy "" przed mapowaniem.
        if (dto.getApiToken() != null && dto.getApiToken().isBlank()) {
            dto.setApiToken(null);
        }
        mapper.update(dto, account);
        return mapper.toDto(repository.save(account));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Lekki test poprawności konta (subdomena + token). Wykonuje pojedyncze
     * GET na faktury bez filtra firmy. Nie zapisuje wyników, tylko sprawdza,
     * czy uwierzytelnienie przechodzi. Zwraca {@code ok=true} przy sukcesie
     * lub {@code ok=false} z komunikatem błędu.
     */
    public Map<String, Object> testConnection(Long id) {
        FakturowniaAccount account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        try {
            fakturowniaClient.fetchInvoices(account, null, "this_month", 1, null);
            return Map.of("ok", true, "message", "Połączenie OK");
        } catch (HttpStatusCodeException e) {
            // Fakturownia zwraca JSON z polem "message" — wyciągnij go zamiast pokazywać raw response.
            String fakturowniaMessage = extractFakturowniaMessage(e.getResponseBodyAsString());
            int status = e.getStatusCode().value();
            String hint = switch (status) {
                case 401 -> "Token nieprawidłowy lub wygasł. Sprawdź w Fakturowni: Ustawienia → Konto → Integracja → Klucze API.";
                case 403 -> "Brak uprawnień. Token nie ma dostępu do tej firmy.";
                case 404 -> "Subdomena nie istnieje. Sprawdź pisownię subdomeny.";
                default -> "Błąd HTTP " + status;
            };
            String full = hint + (fakturowniaMessage != null ? " (" + fakturowniaMessage + ")" : "");
            log.warn("Fakturownia connection test failed for account={}: status={} body={}",
                    id, status, e.getResponseBodyAsString());
            return Map.of("ok", false, "message", full);
        } catch (RestClientException e) {
            log.warn("Fakturownia connection test failed for account={}: {}", id, e.getMessage());
            return Map.of("ok", false,
                    "message", "Brak połączenia z Fakturownią: " + (e.getMessage() != null ? e.getMessage() : "nieznany błąd"));
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String extractFakturowniaMessage(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = MAPPER.readTree(body);
            JsonNode msg = node.get("message");
            if (msg != null && !msg.isNull()) return msg.asText();
        } catch (Exception ignored) { /* nie JSON */ }
        return null;
    }
}
