package wh.plus.crm.service.fakturownia;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.invoice.FakturowniaDictionaryItemDTO;
import wh.plus.crm.model.invoice.FakturowniaAccount;
import wh.plus.crm.repository.FakturowniaAccountRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only słowniki z Fakturowni do napełniania dropdownów w UI.
 *
 * <p><b>Constraint:</b> serwis NIGDY nic nie zapisuje do Fakturowni —
 * wszystkie metody to opakowania na metody GET z {@link FakturowniaClient}.</p>
 */
@Service
@RequiredArgsConstructor
public class FakturowniaDictionaryService {

    private final FakturowniaClient fakturowniaClient;
    private final FakturowniaAccountRepository accountRepository;

    public List<FakturowniaDictionaryItemDTO> getDepartments(Long accountId) {
        FakturowniaAccount account = loadAccount(accountId);
        JsonNode accountJson = fakturowniaClient.fetchAccount(account);
        List<FakturowniaDictionaryItemDTO> result = new ArrayList<>();
        if (accountJson == null) return result;

        // Fakturownia zwraca firmy w polu "departments" (czasem nazwane "companies").
        JsonNode departments = firstNonNull(
                accountJson.get("departments"),
                accountJson.get("companies"));
        if (departments == null || !departments.isArray()) return result;

        for (JsonNode dept : departments) {
            Long id = optLong(dept, "id");
            String name = firstNonBlankText(dept, "name", "company_name", "shortcut");
            if (id != null) {
                result.add(new FakturowniaDictionaryItemDTO(id, name != null ? name : ("Firma #" + id)));
            }
        }
        return result;
    }

    public List<FakturowniaDictionaryItemDTO> getCategories(Long accountId, Long companyId) {
        FakturowniaAccount account = loadAccount(accountId);
        List<JsonNode> categories = fakturowniaClient.fetchCategories(account, companyId);
        List<FakturowniaDictionaryItemDTO> result = new ArrayList<>(categories.size());
        for (JsonNode cat : categories) {
            Long id = optLong(cat, "id");
            String name = firstNonBlankText(cat, "name", "title");
            if (id != null) {
                result.add(new FakturowniaDictionaryItemDTO(id, name != null ? name : ("Kategoria #" + id)));
            }
        }
        return result;
    }

    public List<FakturowniaDictionaryItemDTO> getProjects(Long accountId, Long companyId) {
        FakturowniaAccount account = loadAccount(accountId);
        List<JsonNode> projects = fakturowniaClient.fetchProjects(account, companyId);
        List<FakturowniaDictionaryItemDTO> result = new ArrayList<>(projects.size());
        for (JsonNode proj : projects) {
            Long id = optLong(proj, "id");
            String name = firstNonBlankText(proj, "name", "title");
            if (id != null) {
                result.add(new FakturowniaDictionaryItemDTO(id, name != null ? name : ("Projekt #" + id)));
            }
        }
        return result;
    }

    private FakturowniaAccount loadAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    }

    private static Long optLong(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v != null && !v.isNull() ? v.asLong() : null;
    }

    private static String firstNonBlankText(JsonNode n, String... fields) {
        for (String f : fields) {
            JsonNode v = n.get(f);
            if (v != null && !v.isNull()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) return s;
            }
        }
        return null;
    }

    private static JsonNode firstNonNull(JsonNode... nodes) {
        for (JsonNode n : nodes) {
            if (n != null && !n.isNull()) return n;
        }
        return null;
    }
}
