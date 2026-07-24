package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.invoice.FakturowniaDictionaryItemDTO;
import wh.plus.crm.service.fakturownia.FakturowniaDictionaryService;

import java.util.List;

/**
 * Słownikowe endpointy proxy-jące read-only do Fakturowni.
 * Wszystkie zwracają lekkie {@code {id, name}} do dropdownów w UI.
 *
 * <p>Te endpointy NIE wykonują zapisów — tylko opakowują GET-y na Fakturownię.</p>
 */
@RestController
@RequestMapping("/fakturownia/accounts/{accountId}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class FakturowniaDictionaryController {

    private final FakturowniaDictionaryService service;

    @GetMapping("/departments")
    public ResponseEntity<List<FakturowniaDictionaryItemDTO>> departments(@PathVariable Long accountId) {
        return ResponseEntity.ok(service.getDepartments(accountId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<FakturowniaDictionaryItemDTO>> categories(
            @PathVariable Long accountId,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(service.getCategories(accountId, companyId));
    }

    @GetMapping("/projects")
    public ResponseEntity<List<FakturowniaDictionaryItemDTO>> projects(
            @PathVariable Long accountId,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(service.getProjects(accountId, companyId));
    }
}
