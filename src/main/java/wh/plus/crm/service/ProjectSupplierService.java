package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.supplier.SupplierDTO;
import wh.plus.crm.mapper.SupplierMapper;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.supplier.Supplier;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.repository.SupplierRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectSupplierService {

    private final ProjectRepository projectRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Transactional(readOnly = true)
    public List<SupplierDTO> list(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        return project.getSuppliers().stream().map(supplierMapper::toDto).toList();
    }

    @Transactional
    public void attach(Long projectId, Long supplierId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        if (project.getSuppliers().stream().noneMatch(s -> s.getId().equals(supplierId))) {
            project.getSuppliers().add(supplier);
            projectRepository.save(project);
        }
    }

    @Transactional
    public void detach(Long projectId, Long supplierId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.getSuppliers().removeIf(s -> s.getId().equals(supplierId));
        projectRepository.save(project);
    }
}
