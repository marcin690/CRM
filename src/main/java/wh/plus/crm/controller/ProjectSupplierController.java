package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.supplier.SupplierDTO;
import wh.plus.crm.service.ProjectSupplierService;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/suppliers")
@RequiredArgsConstructor
public class ProjectSupplierController {

    private final ProjectSupplierService service;

    @GetMapping
    public ResponseEntity<List<SupplierDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping("/{supplierId}")
    public ResponseEntity<Void> attach(@PathVariable Long projectId, @PathVariable Long supplierId) {
        service.attach(projectId, supplierId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{supplierId}")
    public ResponseEntity<Void> detach(@PathVariable Long projectId, @PathVariable Long supplierId) {
        service.detach(projectId, supplierId);
        return ResponseEntity.noContent().build();
    }
}
