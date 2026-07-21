package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.order.OrderDTO;
import wh.plus.crm.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @GetMapping
    public ResponseEntity<List<OrderDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping
    public ResponseEntity<OrderDTO> create(@PathVariable Long projectId, @RequestBody OrderDTO dto) {
        return ResponseEntity.ok(service.create(projectId, dto));
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderDTO> update(@PathVariable Long projectId, @PathVariable Long orderId,
                                           @RequestBody OrderDTO dto) {
        return ResponseEntity.ok(service.update(orderId, dto));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long orderId) {
        service.delete(orderId);
        return ResponseEntity.noContent().build();
    }
}
