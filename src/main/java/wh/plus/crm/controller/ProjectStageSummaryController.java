package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.project.ProjectStageSummaryDTO;
import wh.plus.crm.service.ProjectStageService;

import java.util.List;

/** Zbiorczy przegląd etapów wielu projektów — pod wizualizację postępu na liście projektów. */
@RestController
@RequestMapping("/project-stage-summaries")
@RequiredArgsConstructor
public class ProjectStageSummaryController {

    private final ProjectStageService service;

    @GetMapping
    public ResponseEntity<List<ProjectStageSummaryDTO>> summaries(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(service.summaries(ids));
    }
}
