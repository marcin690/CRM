package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.order.AdditionalCostDTO;
import wh.plus.crm.service.AdditionalCostService;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/additional-costs")
@RequiredArgsConstructor
public class AdditionalCostController {

    private final AdditionalCostService service;

    @GetMapping
    public ResponseEntity<List<AdditionalCostDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping
    public ResponseEntity<AdditionalCostDTO> create(@PathVariable Long projectId, @RequestBody AdditionalCostDTO dto) {
        return ResponseEntity.ok(service.create(projectId, dto));
    }

    @PatchMapping("/{costId}")
    public ResponseEntity<AdditionalCostDTO> update(@PathVariable Long projectId, @PathVariable Long costId,
                                                    @RequestBody AdditionalCostDTO dto) {
        return ResponseEntity.ok(service.update(costId, dto));
    }

    @DeleteMapping("/{costId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long costId) {
        service.delete(costId);
        return ResponseEntity.noContent().build();
    }
}
