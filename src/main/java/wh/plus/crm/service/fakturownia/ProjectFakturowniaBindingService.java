package wh.plus.crm.service.fakturownia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.invoice.ProjectFakturowniaBindingDTO;
import wh.plus.crm.mapper.ProjectFakturowniaBindingMapper;
import wh.plus.crm.model.invoice.FakturowniaAccount;
import wh.plus.crm.model.invoice.ProjectFakturowniaBinding;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.FakturowniaAccountRepository;
import wh.plus.crm.repository.ProjectFakturowniaBindingRepository;
import wh.plus.crm.repository.ProjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectFakturowniaBindingService {

    private final ProjectFakturowniaBindingRepository repository;
    private final ProjectRepository projectRepository;
    private final FakturowniaAccountRepository accountRepository;
    private final ProjectFakturowniaBindingMapper mapper;

    public List<ProjectFakturowniaBindingDTO> findByProject(Long projectId) {
        return repository.findAllByProject_Id(projectId).stream().map(mapper::toDto).toList();
    }

    public ProjectFakturowniaBindingDTO create(Long projectId, ProjectFakturowniaBindingDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        FakturowniaAccount account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + dto.getAccountId()));

        ProjectFakturowniaBinding binding = mapper.toEntity(dto);
        binding.setProject(project);
        binding.setAccount(account);
        return mapper.toDto(repository.save(binding));
    }

    public ProjectFakturowniaBindingDTO update(Long bindingId, ProjectFakturowniaBindingDTO dto) {
        ProjectFakturowniaBinding binding = repository.findById(bindingId)
                .orElseThrow(() -> new IllegalArgumentException("Binding not found: " + bindingId));
        mapper.update(dto, binding);
        if (dto.getAccountId() != null && !dto.getAccountId().equals(binding.getAccount().getId())) {
            FakturowniaAccount account = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + dto.getAccountId()));
            binding.setAccount(account);
        }
        return mapper.toDto(repository.save(binding));
    }

    public void delete(Long bindingId) {
        repository.deleteById(bindingId);
    }
}
