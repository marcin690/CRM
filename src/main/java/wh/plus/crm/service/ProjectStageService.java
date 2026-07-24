package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.project.ProjectStageDTO;
import wh.plus.crm.dto.project.ProjectStageSummaryDTO;
import wh.plus.crm.dto.project.StageTaskDTO;
import wh.plus.crm.mapper.ProjectStageMapper;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.project.ProjectStage;
import wh.plus.crm.model.project.StageTask;
import wh.plus.crm.repository.AdditionalCostRepository;
import wh.plus.crm.repository.MontageRepository;
import wh.plus.crm.repository.OrderRepository;
import wh.plus.crm.repository.ProjectFakturowniaBindingRepository;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.repository.ProjectStageRepository;
import wh.plus.crm.repository.StageTaskRepository;
import wh.plus.crm.repository.UserRepository;
import wh.plus.crm.service.fakturownia.ProjectFinanceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectStageService {

    /** Standardowy szablon etapów kontraktu hotelowego (deklarowany per projekt, można zmieniać). */
    private static final List<String> TEMPLATE = List.of(
            "Przygotowanie", "Rysunki i akceptacja", "Produkcja", "Transport", "Montaż", "Odbiór");

    private final ProjectStageRepository stageRepository;
    private final ProjectRepository projectRepository;
    private final OrderRepository orderRepository;
    private final AdditionalCostRepository additionalCostRepository;
    private final MontageRepository montageRepository;
    private final ProjectFakturowniaBindingRepository bindingRepository;
    private final StageTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectFinanceService financeService;
    private final ProjectStageMapper mapper;

    @Transactional(readOnly = true)
    public List<ProjectStageDTO> list(Long projectId) {
        String me = currentUsername();
        return stageRepository.findAllByProject_IdOrderBySortOrderAscIdAsc(projectId).stream()
                .map(mapper::toDto)
                .peek(dto -> dto.setTasks(dto.getTasks().stream()
                        .filter(t -> !t.isPrivateOnly() || (me != null && me.equals(t.getCreatedBy())))
                        .toList()))
                .toList();
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    /** Przegląd etapów dla wielu projektów naraz (jedno zapytanie) — pod wizualizację na liście. */
    @Transactional(readOnly = true)
    public List<ProjectStageSummaryDTO> summaries(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        java.util.Map<Long, ProjectStageSummaryDTO> map = new java.util.LinkedHashMap<>();
        for (Long id : ids) {
            ProjectStageSummaryDTO d = new ProjectStageSummaryDTO();
            d.setProjectId(id);
            try { d.setFinancialPercent(financeService.getFinance(id).getCompletionPercent()); } catch (Exception ignored) { /* brak wiązań/danych */ }
            map.put(id, d);
        }
        for (ProjectStage s : stageRepository.findByProject_IdInOrderBySortOrderAscIdAsc(ids)) {
            ProjectStageSummaryDTO d = map.get(s.getProject().getId());
            if (d == null) continue;
            d.setTotal(d.getTotal() + 1);
            boolean closed = s.getClosedAt() != null;
            if (closed) d.setClosed(d.getClosed() + 1);
            d.getStages().add(new ProjectStageSummaryDTO.Stage(s.getName(), s.getSortOrder(), closed));
        }
        return new java.util.ArrayList<>(map.values());
    }

    @Transactional
    public ProjectStageDTO create(Long projectId, ProjectStageDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectStage stage = new ProjectStage();
        stage.setProject(project);
        mapper.update(dto, stage);
        applyResponsible(stage, dto.getResponsibleUserId());
        if (stage.getSortOrder() == null) stage.setSortOrder(nextOrder(projectId));
        return mapper.toDto(stageRepository.save(stage));
    }

    @Transactional
    public ProjectStageDTO update(Long stageId, ProjectStageDTO dto) {
        ProjectStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
        assertOpen(stage);
        mapper.update(dto, stage);
        applyResponsible(stage, dto.getResponsibleUserId());
        return mapper.toDto(stageRepository.save(stage));
    }

    @Transactional
    public void delete(Long stageId) {
        orderRepository.clearStage(stageId);          // odpięcie zamówień od usuwanego etapu
        additionalCostRepository.clearStage(stageId); // odpięcie kosztów od usuwanego etapu
        montageRepository.clearStage(stageId);        // odpięcie montaży od usuwanego etapu
        bindingRepository.clearStage(stageId);        // odpięcie wiązań faktur (finanse etapu)
        stageRepository.deleteById(stageId);
    }

    /** Zamknięcie etapu z komentarzem podsumowującym. */
    @Transactional
    public ProjectStageDTO close(Long stageId, String comment) {
        ProjectStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
        stage.setStatus("Zakończony");
        stage.setClosedAt(LocalDateTime.now());
        stage.setClosedComment(comment);
        return mapper.toDto(stageRepository.save(stage));
    }

    /** Ponowne otwarcie zamkniętego etapu. */
    @Transactional
    public ProjectStageDTO reopen(Long stageId) {
        ProjectStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
        stage.setStatus("W toku");
        stage.setClosedAt(null);
        stage.setClosedComment(null);
        return mapper.toDto(stageRepository.save(stage));
    }

    /** Zasiewa standardowy szablon etapów, jeśli projekt nie ma jeszcze żadnego. */
    @Transactional
    public List<ProjectStageDTO> applyTemplate(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        if (stageRepository.existsByProject_Id(projectId)) {
            return list(projectId);
        }
        int i = 1;
        for (String name : TEMPLATE) {
            ProjectStage stage = new ProjectStage();
            stage.setProject(project);
            stage.setName(name);
            stage.setSortOrder(i++);
            stage.setStatus("Nowy");
            // Montaż NIE jest już zadaniem — jest osobną encją (zakładka Montaż + timeline),
            // dlatego etap "Montaż" nie dostaje automatycznie zadania montażu.
            stageRepository.save(stage);
        }
        return list(projectId);
    }

    // ---- Zadania (checklista, montaż) ----

    @Transactional
    public StageTaskDTO addTask(Long stageId, StageTaskDTO dto) {
        ProjectStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
        assertOpen(stage);
        StageTask task = new StageTask();
        task.setStage(stage);
        mapper.updateTask(dto, task);
        applyTaskResponsible(task, dto.getResponsibleUserId());
        task.setSortOrder(nextTaskOrder(stageId));
        return mapper.toTaskDto(taskRepository.save(task));
    }

    @Transactional
    public StageTaskDTO updateTask(Long taskId, StageTaskDTO dto) {
        StageTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        assertOpen(task.getStage());
        mapper.updateTask(dto, task);
        applyTaskResponsible(task, dto.getResponsibleUserId());
        return mapper.toTaskDto(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long taskId) {
        StageTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        assertOpen(task.getStage());
        taskRepository.delete(task);
    }

    // ---- helpers ----

    /** Zamknięty etap jest tylko do odczytu — zmiany wymagają wcześniejszego „Otwórz ponownie". */
    private void assertOpen(ProjectStage stage) {
        if (stage.getClosedAt() != null) {
            throw new IllegalStateException("Etap jest zamknięty — otwórz go ponownie, aby edytować.");
        }
    }

    private void applyResponsible(ProjectStage stage, Long userId) {
        if (userId == null) {
            stage.setResponsibleUser(null);
            return;
        }
        stage.setResponsibleUser(userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)));
    }

    private void applyTaskResponsible(StageTask task, Long userId) {
        if (userId == null) {
            task.setResponsibleUser(null);
            return;
        }
        task.setResponsibleUser(userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)));
    }

    private Integer nextOrder(Long projectId) {
        return stageRepository.findAllByProject_IdOrderBySortOrderAscIdAsc(projectId).stream()
                .map(ProjectStage::getSortOrder).filter(Objects::nonNull)
                .max(Integer::compareTo).map(o -> o + 1).orElse(1);
    }

    private Integer nextTaskOrder(Long stageId) {
        return taskRepository.findAllByStage_IdOrderBySortOrderAscIdAsc(stageId).stream()
                .map(StageTask::getSortOrder).filter(Objects::nonNull)
                .max(Integer::compareTo).map(o -> o + 1).orElse(1);
    }
}
