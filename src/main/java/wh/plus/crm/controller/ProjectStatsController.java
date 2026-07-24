package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.order.ProjectStatsDTO;
import wh.plus.crm.service.ProjectStatsService;

@RestController
@RequestMapping("/projects/{projectId}/stats")
@RequiredArgsConstructor
public class ProjectStatsController {

    private final ProjectStatsService service;

    @GetMapping
    public ResponseEntity<ProjectStatsDTO> stats(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getStats(projectId));
    }
}
