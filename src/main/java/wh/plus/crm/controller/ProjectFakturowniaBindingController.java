package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.invoice.ProjectFakturowniaBindingDTO;
import wh.plus.crm.service.fakturownia.ProjectFakturowniaBindingService;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/fakturownia-bindings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class ProjectFakturowniaBindingController {

    private final ProjectFakturowniaBindingService service;

    @GetMapping
    public ResponseEntity<List<ProjectFakturowniaBindingDTO>> findByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.findByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectFakturowniaBindingDTO> create(
            @PathVariable Long projectId,
            @RequestBody ProjectFakturowniaBindingDTO dto) {
        return ResponseEntity.ok(service.create(projectId, dto));
    }

    @PatchMapping("/{bindingId}")
    public ResponseEntity<ProjectFakturowniaBindingDTO> update(
            @PathVariable Long projectId,
            @PathVariable Long bindingId,
            @RequestBody ProjectFakturowniaBindingDTO dto) {
        return ResponseEntity.ok(service.update(bindingId, dto));
    }

    @DeleteMapping("/{bindingId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long bindingId) {
        service.delete(bindingId);
        return ResponseEntity.noContent().build();
    }
}
