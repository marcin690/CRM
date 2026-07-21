package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.project.ConstructionLogCommentDTO;
import wh.plus.crm.dto.project.ConstructionLogEntryDTO;
import wh.plus.crm.service.ConstructionLogService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/construction-log")
@RequiredArgsConstructor
public class ConstructionLogController {

    private final ConstructionLogService service;

    @GetMapping
    public ResponseEntity<List<ConstructionLogEntryDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.list(projectId));
    }

    @PostMapping
    public ResponseEntity<ConstructionLogEntryDTO> create(
            @PathVariable Long projectId,
            @RequestBody ConstructionLogEntryDTO dto) {
        return ResponseEntity.ok(service.create(projectId, dto));
    }

    @PatchMapping("/{entryId}")
    public ResponseEntity<ConstructionLogEntryDTO> update(
            @PathVariable Long projectId,
            @PathVariable Long entryId,
            @RequestBody ConstructionLogEntryDTO dto) {
        return ResponseEntity.ok(service.update(entryId, dto));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long entryId) {
        service.delete(entryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entryId}/comments")
    public ResponseEntity<ConstructionLogCommentDTO> addComment(
            @PathVariable Long projectId,
            @PathVariable Long entryId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.addComment(entryId, body.get("content")));
    }

    @DeleteMapping("/{entryId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long projectId,
            @PathVariable Long entryId,
            @PathVariable Long commentId) {
        service.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@PathVariable Long projectId) {
        byte[] xlsx = service.exportToExcel(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"dziennik-budowy-projekt-" + projectId + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
