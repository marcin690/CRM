package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.supplier.SupplierDTO;
import wh.plus.crm.mapper.SupplierMapper;
import wh.plus.crm.model.supplier.Supplier;
import wh.plus.crm.repository.SupplierRepository;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;
    private final SupplierMapper mapper;

    public Page<SupplierDTO> list(String search, Pageable pageable) {
        Page<Supplier> page = (search == null || search.isBlank())
                ? repository.findAll(pageable)
                : repository.findByNameContainingIgnoreCaseOrNipContainingIgnoreCase(search, search, pageable);
        return page.map(mapper::toDto);
    }

    public SupplierDTO getById(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id)));
    }

    public SupplierDTO create(SupplierDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Nazwa dostawcy jest wymagana");
        }
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    public SupplierDTO update(Long id, SupplierDTO dto) {
        Supplier existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        mapper.update(dto, existing);
        return mapper.toDto(repository.save(existing));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
