package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.sharepoint.ProjectSharePointFileDTO;
import wh.plus.crm.service.sharepoint.SharePointService;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/sharepoint-files")
@RequiredArgsConstructor
public class ProjectSharePointController {

    private final SharePointService service;

    @GetMapping
    public ResponseEntity<List<ProjectSharePointFileDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.listProjectFiles(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectSharePointFileDTO> attach(
            @PathVariable Long projectId,
            @RequestBody ProjectSharePointFileDTO dto) {
        return ResponseEntity.ok(service.attach(projectId, dto));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> detach(@PathVariable Long projectId, @PathVariable Long fileId) {
        service.detach(fileId);
        return ResponseEntity.noContent().build();
    }
}
