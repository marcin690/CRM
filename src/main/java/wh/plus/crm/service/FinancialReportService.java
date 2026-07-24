package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.invoice.ProjectFinanceDTO;
import wh.plus.crm.dto.project.FinancialOverviewDTO;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.service.fakturownia.ProjectFinanceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Raport finansowy projektów (K2026): wszystkie projekty z rozliczeniem plan vs realizacja. */
@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private final ProjectRepository projectRepository;
    private final ProjectFinanceService financeService;

    @Transactional(readOnly = true)
    public List<FinancialOverviewDTO> overview() {
        List<FinancialOverviewDTO> out = new ArrayList<>();
        for (Project p : projectRepository.findAll()) {
            FinancialOverviewDTO d = new FinancialOverviewDTO();
            d.setProjectId(p.getId());
            d.setName(p.getName());
            d.setTeamName(p.getSalesTeam() != null ? p.getSalesTeam().getName() : null);
            d.setStatus(p.getStatus());
            d.setContractValue(p.getProjectNetValue());
            d.setDeclaredMargin(p.getDeclaredMargin());

            BigDecimal value = p.getProjectNetValue() != null ? BigDecimal.valueOf(p.getProjectNetValue()) : BigDecimal.ZERO;
            d.setPlannedProfit(p.getDeclaredMargin() != null
                    ? value.multiply(p.getDeclaredMargin()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            ProjectFinanceDTO f = null;
            try { f = financeService.getFinance(p.getId()); } catch (Exception ignored) { /* brak danych finansowych */ }
            BigDecimal rev = f != null && f.getSumRevenueNet() != null ? f.getSumRevenueNet() : BigDecimal.ZERO;
            d.setRevenueNet(rev);
            d.setCostNet(f != null && f.getSumCostNet() != null ? f.getSumCostNet() : BigDecimal.ZERO);
            d.setProfitNet(f != null && f.getMarginNet() != null ? f.getMarginNet() : BigDecimal.ZERO);
            d.setCompletionPercent(f != null && f.getCompletionPercent() != null ? f.getCompletionPercent() : BigDecimal.ZERO);
            d.setToInvoice(value.subtract(rev));
            out.add(d);
        }
        return out;
    }
}
