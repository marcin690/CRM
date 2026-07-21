package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.supplier.SupplierDTO;
import wh.plus.crm.service.SupplierService;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<SupplierDTO>>> list(
            @RequestParam(required = false) String search,
            Pageable pageable,
            PagedResourcesAssembler<SupplierDTO> assembler) {
        Page<SupplierDTO> page = service.list(search, pageable);
        return ResponseEntity.ok(assembler.toModel(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<SupplierDTO> create(@RequestBody SupplierDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SupplierDTO> update(@PathVariable Long id, @RequestBody SupplierDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
