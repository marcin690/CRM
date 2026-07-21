package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.montage.MontageDTO;
import wh.plus.crm.mapper.MontageMapper;
import wh.plus.crm.model.crew.Crew;
import wh.plus.crm.model.montage.Montage;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.CrewRepository;
import wh.plus.crm.repository.MontageRepository;
import wh.plus.crm.repository.ProjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MontageService {

    private final MontageRepository montageRepository;
    private final ProjectRepository projectRepository;
    private final CrewRepository crewRepository;
    private final MontageMapper mapper;

    @Transactional(readOnly = true)
    public List<MontageDTO> list(Long projectId) {
        return montageRepository.findAllByProject_IdOrderByStartDateAsc(projectId)
                .stream().map(mapper::toDto).toList();
    }

    /** Wszystkie montaże (wszystkie projekty) — pod harmonogram / widok Gantt. */
    @Transactional(readOnly = true)
    public List<MontageDTO> schedule() {
        return montageRepository.findAllByOrderByStartDateAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public MontageDTO create(Long projectId, MontageDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Montage montage = new Montage();
        montage.setProject(project);
        mapper.update(dto, montage);
        applyCrew(montage, dto.getCrewId());
        return mapper.toDto(montageRepository.save(montage));
    }

    @Transactional
    public MontageDTO update(Long id, MontageDTO dto) {
        Montage montage = montageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Montage not found: " + id));
        mapper.update(dto, montage);
        applyCrew(montage, dto.getCrewId());
        return mapper.toDto(montageRepository.save(montage));
    }

    @Transactional
    public void delete(Long id) {
        montageRepository.deleteById(id);
    }

    private void applyCrew(Montage montage, Long crewId) {
        if (crewId == null) {
            montage.setCrew(null);
            return;
        }
        Crew crew = crewRepository.findById(crewId)
                .orElseThrow(() -> new IllegalArgumentException("Crew not found: " + crewId));
        montage.setCrew(crew);
    }
}
