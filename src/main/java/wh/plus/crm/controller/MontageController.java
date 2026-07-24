package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.montage.MontageDTO;
import wh.plus.crm.service.MontageService;

import java.util.List;

/** Montaże w kontekście projektu. Widok globalny (harmonogram) jest w {@link MontageScheduleController}. */
@RestController
@RequestMapping("/projects/{projectId}/montages")
@RequiredArgsConstructor
public class MontageController {

    private final MontageService service;

    @GetMapping
    public ResponseEntity<List<MontageDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping
    public ResponseEntity<MontageDTO> create(@PathVariable Long projectId, @RequestBody MontageDTO dto) {
        return ResponseEntity.ok(service.create(projectId, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MontageDTO> update(@PathVariable Long projectId, @PathVariable Long id, @RequestBody MontageDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
