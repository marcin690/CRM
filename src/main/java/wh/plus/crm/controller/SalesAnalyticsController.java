package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import wh.plus.crm.dto.dashboard.RepDetailDTO;
import wh.plus.crm.dto.dashboard.SalesAnalyticsDTO;
import wh.plus.crm.dto.dashboard.SalesInsightsDTO;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.UserRepository;
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
    private final UserRepository userRepository;

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
            @RequestParam(required = false, defaultValue = "false") boolean refresh,
            @RequestParam(required = false) Long teamId
    ) {
        currentUserService.requireReports();
        // Admin: może wybrać analizę ogólną (teamId=null) lub konkretnego zespołu (teamId=X).
        // Pozostali: zawsze wymuszony własny zespół, parametr klienta ignorowany.
        Long effectiveTeam = currentUserService.isAdmin() ? teamId : currentUserService.scopeTeamId();
        return ResponseEntity.ok(salesInsightsService.getInsights(dateFrom, dateTo, refresh, effectiveTeam));
    }

    /** Drill-down handlowca. Dostęp: admin lub manager/pracownik z tego samego zespołu co dany handlowiec. */
    @GetMapping("/rep/{userId}")
    public ResponseEntity<RepDetailDTO> repDetail(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        currentUserService.requireReports();
        User rep = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono handlowca"));
        currentUserService.assertTeamAccess(rep.getTeam() != null ? rep.getTeam().getId() : null);
        return ResponseEntity.ok(salesAnalyticsService.getRepDetail(userId, dateFrom, dateTo));
    }
}
