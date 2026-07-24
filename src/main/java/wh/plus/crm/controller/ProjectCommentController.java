package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.project.ProjectCommentDTO;
import wh.plus.crm.service.ProjectCommentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/comments")
@RequiredArgsConstructor
public class ProjectCommentController {

    private final ProjectCommentService service;

    @GetMapping
    public ResponseEntity<List<ProjectCommentDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectCommentDTO> create(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.create(projectId, body.get("content")));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long commentId) {
        service.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
