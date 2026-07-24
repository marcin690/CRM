package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.furniture.FurnitureItemDTO;
import wh.plus.crm.service.FurnitureService;

import java.util.List;
import java.util.Map;

/** Karta mebli projektu (rozpiska / karta informacyjna). */
@RestController
@RequestMapping("/projects/{projectId}/furniture")
@RequiredArgsConstructor
public class FurnitureController {

    private final FurnitureService service;

    @GetMapping
    public ResponseEntity<List<FurnitureItemDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping
    public ResponseEntity<FurnitureItemDTO> create(@PathVariable Long projectId, @RequestBody FurnitureItemDTO dto) {
        return ResponseEntity.ok(service.create(projectId, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FurnitureItemDTO> update(@PathVariable Long projectId, @PathVariable Long id, @RequestBody FurnitureItemDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Wyślij wybrane pozycje do zamówienia / zleć produkcję. Body: {"ids":[1,2,3]}. */
    @PostMapping("/send-to-order")
    public ResponseEntity<List<FurnitureItemDTO>> sendToOrder(@PathVariable Long projectId, @RequestBody Map<String, List<Long>> body) {
        return ResponseEntity.ok(service.sendToOrder(projectId, body.getOrDefault("ids", List.of())));
    }
}
