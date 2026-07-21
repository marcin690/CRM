package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.order.AdditionalCostDTO;
import wh.plus.crm.mapper.AdditionalCostMapper;
import wh.plus.crm.model.order.AdditionalCost;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.AdditionalCostRepository;
import wh.plus.crm.repository.ProjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdditionalCostService {

    private final AdditionalCostRepository repository;
    private final ProjectRepository projectRepository;
    private final AdditionalCostMapper mapper;

    public List<AdditionalCostDTO> list(Long projectId) {
        return repository.findAllByProject_IdOrderByCostDateDesc(projectId)
                .stream().map(mapper::toDto).toList();
    }

    public AdditionalCostDTO create(Long projectId, AdditionalCostDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        AdditionalCost cost = mapper.toEntity(dto);
        cost.setProject(project);
        return mapper.toDto(repository.save(cost));
    }

    public AdditionalCostDTO update(Long costId, AdditionalCostDTO dto) {
        AdditionalCost cost = repository.findById(costId)
                .orElseThrow(() -> new IllegalArgumentException("Cost not found: " + costId));
        mapper.update(dto, cost);
        return mapper.toDto(repository.save(cost));
    }

    public void delete(Long costId) {
        repository.deleteById(costId);
    }
}
