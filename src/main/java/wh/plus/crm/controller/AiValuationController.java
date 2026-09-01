package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wh.plus.crm.dto.aivaluation.AiAgentDto;
import wh.plus.crm.dto.aivaluation.ValuationJobDto;
import wh.plus.crm.service.aivaluation.ValuationService;

import java.util.List;
import java.util.Map;

/**
 * Moduł Narzędzia → Wyceny AI. Dostęp: każdy zalogowany użytkownik
 * (zgodnie z {@code anyRequest().authenticated()} w SecurityConfig).
 */
@RestController
@RequestMapping("/ai-valuations")
@RequiredArgsConstructor
public class AiValuationController {

    private final ValuationService valuationService;

    /** Lista dostępnych agentów do wyboru w formularzu. */
    @GetMapping("/agents")
    public List<AiAgentDto> agents() {
        return valuationService.listAgents();
    }

    /** Lista zadań wyceny (najnowsze na górze). */
    @GetMapping
    public List<ValuationJobDto> list() {
        return valuationService.listJobs();
    }

    /** Liczba wycen w toku — dla globalnego wskaźnika w menu. */
    @GetMapping("/active-count")
    public Map<String, Long> activeCount() {
        return Map.of("count", valuationService.activeCount());
    }

    /** Szczegół zadania wraz z wątkiem czatu. */
    @GetMapping("/{id}")
    public ValuationJobDto get(@PathVariable Long id) {
        return valuationService.getJob(id);
    }

    /** Utworzenie zadań wyceny — jeden plik = jedna pozycja. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ValuationJobDto>> create(
            @RequestParam("agentCode") String agentCode,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "materialClass", required = false) String materialClass,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(valuationService.createJobs(agentCode, note, materialClass, quantity, files));
    }

    /** Kontynuacja rozmowy z agentem w ramach zadania. */
    @PostMapping(value = "/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValuationJobDto sendMessage(
            @PathVariable Long id,
            @RequestParam("query") String query,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return valuationService.continueChat(id, query, files);
    }

    /** Porównanie gotowej wyceny z ofertą dostawcy (opis i/lub pliki). */
    @PostMapping(value = "/{id}/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValuationJobDto compare(
            @PathVariable Long id,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return valuationService.compareWithOffer(id, description, files);
    }

    /** Ocena/komentarz użytkownika — przekazywane do Dify (rating: "like"/"dislike"). */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> feedback(@PathVariable Long id, @RequestBody FeedbackRequest req) {
        valuationService.submitFeedback(id, req.rating(), req.comment());
        return ResponseEntity.ok().build();
    }

    /** Usunięcie wyceny wraz z historią czatu. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        valuationService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    /** Zmiana tytułu wyceny. */
    @PatchMapping("/{id}/title")
    public ValuationJobDto rename(@PathVariable Long id, @RequestBody RenameRequest req) {
        return valuationService.renameJob(id, req.title());
    }

    public record FeedbackRequest(String rating, String comment) {}
    public record RenameRequest(String title) {}
}
