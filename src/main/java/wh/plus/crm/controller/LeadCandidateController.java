package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.lead.LeadCandidateDto;
import wh.plus.crm.dto.lead.LeadCandidateRunDto;
import wh.plus.crm.model.lead.LeadCandidateStatus;
import wh.plus.crm.service.ailead.LeadCandidateService;

import java.util.List;
import java.util.Map;

/**
 * Zakładka „Automatyczne leady z rynku": uruchomienie agenta, lista kandydatów
 * i decyzje handlowca (Dodaj / Odrzuć).
 */
@RestController
@RequestMapping("/lead-candidates")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LeadCandidateController {

    private final LeadCandidateService service;

    public record GenerateRequest(String tryb, Integer dniNewsy, Integer dniPrzetargi) {}
    public record RejectRequest(String note) {}

    /** Uruchamia przebieg agenta (async). Zwraca status przebiegu (RUNNING). */
    @PostMapping("/generate")
    public ResponseEntity<LeadCandidateRunDto> generate(@RequestBody(required = false) GenerateRequest req,
                                                        Authentication auth) {
        GenerateRequest r = req == null ? new GenerateRequest(null, null, null) : req;
        LeadCandidateRunDto run = service.startRun(r.tryb(), r.dniNewsy(), r.dniPrzetargi(), auth.getName());
        return ResponseEntity.ok(run);
    }

    /** Status ostatniego przebiegu (do pollingu). */
    @GetMapping("/run/latest")
    public ResponseEntity<LeadCandidateRunDto> latestRun() {
        return ResponseEntity.ok(service.latestRun());
    }

    /** Lista kandydatów (domyślnie NEW). */
    @GetMapping
    public ResponseEntity<List<LeadCandidateDto>> list(
            @RequestParam(required = false, defaultValue = "NEW") LeadCandidateStatus status) {
        return ResponseEntity.ok(service.list(status));
    }

    /** Dodaj: tworzy wersję roboczą leada z kandydata. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<Map<String, Long>> accept(@PathVariable Long id, Authentication auth) {
        Long leadId = service.accept(id, auth.getName());
        return ResponseEntity.ok(Map.of("leadId", leadId));
    }

    /** Odrzuć kandydata. */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @RequestBody(required = false) RejectRequest req,
                                       Authentication auth) {
        service.reject(id, req == null ? null : req.note(), auth.getName());
        return ResponseEntity.noContent().build();
    }
}
