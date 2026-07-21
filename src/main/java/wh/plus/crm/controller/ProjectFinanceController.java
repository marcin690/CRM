package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.invoice.ProjectFinanceDTO;
import wh.plus.crm.dto.invoice.StageFinanceDTO;
import wh.plus.crm.service.fakturownia.ProjectFinanceService;

import java.util.List;

@RestController
@RequestMapping("/projects/{id}/finance")
@RequiredArgsConstructor
public class ProjectFinanceController {

    private final ProjectFinanceService projectFinanceService;

    @GetMapping
    public ResponseEntity<ProjectFinanceDTO> getFinance(@PathVariable Long id) {
        return ResponseEntity.ok(projectFinanceService.getFinance(id));
    }

    @GetMapping("/stages")
    public ResponseEntity<List<StageFinanceDTO>> getStageFinances(@PathVariable Long id) {
        return ResponseEntity.ok(projectFinanceService.getStageFinances(id));
    }
}
