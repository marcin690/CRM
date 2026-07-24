package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.project.FinancialOverviewDTO;
import wh.plus.crm.service.FinancialReportService;

import java.util.List;

/** Raport finansowy projektów. */
@RestController
@RequestMapping("/reports/financial")
@RequiredArgsConstructor
public class FinancialReportController {

    private final FinancialReportService service;

    @GetMapping
    public ResponseEntity<List<FinancialOverviewDTO>> overview() {
        return ResponseEntity.ok(service.overview());
    }
}
