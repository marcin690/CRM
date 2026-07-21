package wh.plus.crm.service.fakturownia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.invoice.InvoiceDTO;
import wh.plus.crm.dto.invoice.ProjectFinanceDTO;
import wh.plus.crm.dto.invoice.StageFinanceDTO;
import wh.plus.crm.mapper.InvoiceMapper;
import wh.plus.crm.model.invoice.FakturowniaRole;
import wh.plus.crm.model.invoice.Invoice;
import wh.plus.crm.model.invoice.InvoiceKind;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.InvoiceRepository;
import wh.plus.crm.repository.ProjectRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectFinanceService {

    /**
     * Rodzaje faktur, które NIE są księgowo przychodem/kosztem i muszą zostać
     * wyłączone z agregacji — pokazujemy je w tabeli faktur, ale nie wliczamy
     * do KPI ani do % realizacji.
     *
     * - PROFORMA: dokument handlowy, nie przychód księgowy
     * - RECEIPT (KP): potwierdzenie wpłaty, nie faktura przychodowa
     */
    private static final Set<InvoiceKind> NON_ACCOUNTING_KINDS =
            EnumSet.of(InvoiceKind.PROFORMA, InvoiceKind.RECEIPT);

    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    public ProjectFinanceDTO getFinance(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<Invoice> invoices = invoiceRepository
                .findAllByBinding_Project_IdOrderByIssueDateDesc(projectId);

        BigDecimal sumRevenueNet = BigDecimal.ZERO;
        BigDecimal sumRevenueGross = BigDecimal.ZERO;
        BigDecimal sumCostNet = BigDecimal.ZERO;
        BigDecimal sumCostGross = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            if (inv.getBinding() == null) continue;
            if (NON_ACCOUNTING_KINDS.contains(inv.getKind())) continue;
            FakturowniaRole role = inv.getBinding().getRole();
            BigDecimal net = inv.getNetValue() != null ? inv.getNetValue() : BigDecimal.ZERO;
            BigDecimal gross = inv.getGrossValue() != null ? inv.getGrossValue() : BigDecimal.ZERO;
            if (role == FakturowniaRole.REVENUE) {
                sumRevenueNet = sumRevenueNet.add(net);
                sumRevenueGross = sumRevenueGross.add(gross);
            } else if (role == FakturowniaRole.COST) {
                sumCostNet = sumCostNet.add(net);
                sumCostGross = sumCostGross.add(gross);
            }
        }

        ProjectFinanceDTO dto = new ProjectFinanceDTO();
        dto.setProjectId(projectId);
        dto.setDeclaredValue(project.getProjectNetValue());
        dto.setSumRevenueNet(sumRevenueNet);
        dto.setSumRevenueGross(sumRevenueGross);
        dto.setSumCostNet(sumCostNet);
        dto.setSumCostGross(sumCostGross);
        dto.setMarginNet(sumRevenueNet.subtract(sumCostNet));
        dto.setCompletionPercent(computeCompletionPercent(project.getProjectNetValue(), sumRevenueNet));
        dto.setInvoices(invoices.stream().map(invoiceMapper::toDto).toList());
        return dto;
    }

    /** Finanse w rozbiciu na etapy — dla faktur, których wiązanie ma ustawiony stageId. */
    public List<StageFinanceDTO> getStageFinances(Long projectId) {
        List<Invoice> invoices = invoiceRepository.findAllByBinding_Project_IdOrderByIssueDateDesc(projectId);
        java.util.Map<Long, StageFinanceDTO> map = new java.util.LinkedHashMap<>();
        for (Invoice inv : invoices) {
            if (inv.getBinding() == null) continue;
            Long stageId = inv.getBinding().getStageId();
            if (stageId == null) continue;
            if (NON_ACCOUNTING_KINDS.contains(inv.getKind())) continue;
            StageFinanceDTO d = map.computeIfAbsent(stageId, k -> { StageFinanceDTO x = new StageFinanceDTO(); x.setStageId(k); return x; });
            BigDecimal net = inv.getNetValue() != null ? inv.getNetValue() : BigDecimal.ZERO;
            if (inv.getBinding().getRole() == FakturowniaRole.REVENUE) d.setRevenueNet(d.getRevenueNet().add(net));
            else if (inv.getBinding().getRole() == FakturowniaRole.COST) d.setCostNet(d.getCostNet().add(net));
            d.setInvoiceCount(d.getInvoiceCount() + 1);
        }
        map.values().forEach(d -> d.setMarginNet(d.getRevenueNet().subtract(d.getCostNet())));
        return new java.util.ArrayList<>(map.values());
    }

    private BigDecimal computeCompletionPercent(Long declaredValue, BigDecimal sumRevenueNet) {
        if (declaredValue == null || declaredValue == 0L) return BigDecimal.ZERO;
        BigDecimal declared = BigDecimal.valueOf(declaredValue);
        return sumRevenueNet
                .multiply(BigDecimal.valueOf(100))
                .divide(declared, 2, RoundingMode.HALF_UP);
    }
}
