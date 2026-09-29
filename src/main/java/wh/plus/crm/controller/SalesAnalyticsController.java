package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.dashboard.SalesAnalyticsDTO;
import wh.plus.crm.dto.dashboard.SalesInsightsDTO;
import wh.plus.crm.service.CurrentUserService;
import wh.plus.crm.service.SalesAnalyticsService;
import wh.plus.crm.service.SalesInsightsService;

import java.time.LocalDate;

/**
 * Cockpit sprzedaży — agregaty lead → oferta → umowa dla strony /reports/cockpit.
 * Opcjonalne compareFrom/compareTo zwracają dodatkowy blok KPI dla okresu porównawczego.
 */
@RestController
@RequestMapping("/dashboard/sales-analytics")
@RequiredArgsConstructor
public class SalesAnalyticsController {

    private final SalesAnalyticsService salesAnalyticsService;
    private final SalesInsightsService salesInsightsService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<SalesAnalyticsDTO> getSalesAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate compareFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate compareTo
    ) {
        currentUserService.requireReports();
        Long teamId = currentUserService.scopeTeamId(); // null dla ADMIN
        return ResponseEntity.ok(salesAnalyticsService.getAnalytics(dateFrom, dateTo, compareFrom, compareTo, teamId));
    }

    /** Wnioski AI (Claude) dla tego samego okresu. refresh=true wymusza przeliczenie (pomija cache). */
    @GetMapping("/insights")
    public ResponseEntity<SalesInsightsDTO> getInsights(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "false") boolean refresh
    ) {
        currentUserService.requireReports();
        Long teamId = currentUserService.scopeTeamId();
        return ResponseEntity.ok(salesInsightsService.getInsights(dateFrom, dateTo, refresh, teamId));
    }
}
