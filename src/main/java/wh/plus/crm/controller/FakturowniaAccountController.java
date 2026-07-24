package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.invoice.FakturowniaAccountDTO;
import wh.plus.crm.service.fakturownia.FakturowniaAccountService;
import wh.plus.crm.service.fakturownia.FakturowniaSyncService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fakturownia")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class FakturowniaAccountController {

    private final FakturowniaAccountService service;
    private final FakturowniaSyncService syncService;

    @PostMapping("/sync")
    public ResponseEntity<FakturowniaSyncService.SyncResult> triggerSync() {
        return ResponseEntity.ok(syncService.syncAll());
    }

    /** Diagnostyka (read-only): pokazuje surowy JSON faktury i kategorie, by dobrać dopasowanie. */
    @GetMapping("/accounts/{id}/debug-invoices")
    public ResponseEntity<Map<String, Object>> debugInvoices(
            @PathVariable Long id,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(syncService.debugFirstPage(id, companyId));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<FakturowniaAccountDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<FakturowniaAccountDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping("/accounts")
    public ResponseEntity<FakturowniaAccountDTO> create(@RequestBody FakturowniaAccountDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PatchMapping("/accounts/{id}")
    public ResponseEntity<FakturowniaAccountDTO> update(@PathVariable Long id, @RequestBody FakturowniaAccountDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accounts/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        return ResponseEntity.ok(service.testConnection(id));
    }
}
