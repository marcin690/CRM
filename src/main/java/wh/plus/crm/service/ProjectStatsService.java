package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.invoice.ProjectFinanceDTO;
import wh.plus.crm.dto.order.ProjectStatsDTO;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.order.Order;
import wh.plus.crm.model.order.OrderItem;
import wh.plus.crm.model.order.OrderKind;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.repository.AdditionalCostRepository;
import wh.plus.crm.repository.FurnitureItemRepository;
import wh.plus.crm.repository.MontageRepository;
import wh.plus.crm.repository.OrderRepository;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.service.fakturownia.ProjectFinanceService;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectStatsService {

    private final ProjectRepository projectRepository;
    private final OrderRepository orderRepository;
    private final AdditionalCostRepository additionalCostRepository;
    private final MontageRepository montageRepository;
    private final FurnitureItemRepository furnitureItemRepository;
    private final ProjectFinanceService financeService;

    @Transactional(readOnly = true)
    public ProjectStatsDTO getStats(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        ProjectStatsDTO stats = new ProjectStatsDTO();
        stats.setProjectId(projectId);
        stats.setDeclaredValue(project.getTotalDeclaredValue() != null
                ? project.getTotalDeclaredValue() : project.getProjectNetValue());
        stats.setDeclaredMargin(project.getDeclaredMargin());

        // Oferty powiązane z projektem.
        BigDecimal offersTotal = BigDecimal.ZERO;
        for (Offer o : project.getOffers()) {
            if (o.getTotalPrice() != null) offersTotal = offersTotal.add(o.getTotalPrice());
        }
        stats.setOffersTotal(offersTotal);

        // Zamówienia vs domówienia (wartość z pozycji).
        BigDecimal ordersTotal = BigDecimal.ZERO;
        BigDecimal reordersTotal = BigDecimal.ZERO;
        for (Order order : orderRepository.findAllByProject_IdOrderByIdDesc(projectId)) {
            BigDecimal value = orderValue(order);
            if (order.getOrderKind() == OrderKind.REORDER) reordersTotal = reordersTotal.add(value);
            else ordersTotal = ordersTotal.add(value);
        }
        stats.setOrdersTotal(ordersTotal);
        stats.setReordersTotal(reordersTotal);

        // Koszty dodatkowe z palca.
        BigDecimal additional = additionalCostRepository.findAllByProject_IdOrderByCostDateDesc(projectId)
                .stream()
                .map(c -> c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setAdditionalCostsTotal(additional);

        // Wartości montaży — encja Montage jest jedynym źródłem prawdy o montażu (plan).
        BigDecimal montages = montageRepository.findAllByProject_IdOrderByStartDateAsc(projectId).stream()
                .map(m -> m.getNetValue() != null ? m.getNetValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setMontagesTotal(montages);

        // Meble (rozpiska) do planu — TYLKO pozycje jeszcze niewysłane do zamówienia.
        // Wysłane stają się zamówieniem (Order) i są już policzone w ordersTotal — inaczej dubel.
        BigDecimal furniture = furnitureItemRepository.findAllByProject_IdOrderBySortOrderAscIdAsc(projectId).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getOrdered()))
                .map(f -> f.getUnitPrice() != null
                        ? f.getUnitPrice().multiply(BigDecimal.valueOf(f.getQuantity() != null ? f.getQuantity() : 1))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setFurnitureTotal(furniture);

        // KOSZT PLANU (wycena) — każda pozycja liczona raz, bez dublowania.
        BigDecimal planCost = ordersTotal.add(reordersTotal).add(additional).add(montages).add(furniture);
        stats.setPlanCostTotal(planCost);
        BigDecimal declared = stats.getDeclaredValue() != null
                ? BigDecimal.valueOf(stats.getDeclaredValue()) : BigDecimal.ZERO;
        stats.setPlanMarginNet(declared.subtract(planCost));

        // REALIZACJA — jedyne źródło prawdy: faktury Fakturowni (tylko odczyt).
        ProjectFinanceDTO finance = financeService.getFinance(projectId);
        BigDecimal revenue = finance.getSumRevenueNet() != null ? finance.getSumRevenueNet() : BigDecimal.ZERO;
        BigDecimal fkCost = finance.getSumCostNet() != null ? finance.getSumCostNet() : BigDecimal.ZERO;
        stats.setFakturowniaRevenueNet(revenue);
        stats.setFakturowniaCostNet(fkCost);

        BigDecimal realizationMargin = revenue.subtract(fkCost);
        stats.setRealizationMarginNet(realizationMargin);
        stats.setTotalCostNet(fkCost);          // koszt realizacji = faktury zakupowe
        stats.setMarginNet(realizationMargin);  // marża realizacji = przychód − koszt (faktury)
        return stats;
    }

    private BigDecimal orderValue(Order order) {
        // Płaski model: wartość wpisana wprost ma pierwszeństwo. Starsze zamówienia liczymy z pozycji.
        if (order.getNetValue() != null) return order.getNetValue();
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem it : order.getItems()) {
            if (it.getUnitPrice() == null) continue;
            int qty = it.getQuantity() != null ? it.getQuantity() : 1;
            sum = sum.add(it.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
        }
        return sum;
    }
}
