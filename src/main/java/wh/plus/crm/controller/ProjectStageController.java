package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.project.ProjectStageDTO;
import wh.plus.crm.dto.project.StageTaskDTO;
import wh.plus.crm.service.ProjectStageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/stages")
@RequiredArgsConstructor
public class ProjectStageController {

    private final ProjectStageService service;

    @GetMapping
    public ResponseEntity<List<ProjectStageDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectStageDTO> create(@PathVariable Long projectId, @RequestBody ProjectStageDTO dto) {
        return ResponseEntity.ok(service.create(projectId, dto));
    }

    /** Zasiewa standardowy szablon etapów (tylko gdy projekt nie ma jeszcze żadnych). */
    @PostMapping("/apply-template")
    public ResponseEntity<List<ProjectStageDTO>> applyTemplate(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.applyTemplate(projectId));
    }

    @PatchMapping("/{stageId}")
    public ResponseEntity<ProjectStageDTO> update(@PathVariable Long projectId, @PathVariable Long stageId,
                                                  @RequestBody ProjectStageDTO dto) {
        return ResponseEntity.ok(service.update(stageId, dto));
    }

    @DeleteMapping("/{stageId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long stageId) {
        service.delete(stageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{stageId}/close")
    public ResponseEntity<ProjectStageDTO> close(@PathVariable Long projectId, @PathVariable Long stageId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(service.close(stageId, body != null ? body.get("comment") : null));
    }

    @PostMapping("/{stageId}/reopen")
    public ResponseEntity<ProjectStageDTO> reopen(@PathVariable Long projectId, @PathVariable Long stageId) {
        return ResponseEntity.ok(service.reopen(stageId));
    }

    // ---- Zadania w etapie (checklista, montaż) ----

    @PostMapping("/{stageId}/tasks")
    public ResponseEntity<StageTaskDTO> addTask(@PathVariable Long projectId, @PathVariable Long stageId,
                                                @RequestBody StageTaskDTO dto) {
        return ResponseEntity.ok(service.addTask(stageId, dto));
    }

    @PatchMapping("/{stageId}/tasks/{taskId}")
    public ResponseEntity<StageTaskDTO> updateTask(@PathVariable Long projectId, @PathVariable Long stageId,
                                                   @PathVariable Long taskId, @RequestBody StageTaskDTO dto) {
        return ResponseEntity.ok(service.updateTask(taskId, dto));
    }

    @DeleteMapping("/{stageId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long projectId, @PathVariable Long stageId,
                                           @PathVariable Long taskId) {
        service.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
