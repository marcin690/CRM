package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.furniture.FurnitureItemDTO;
import wh.plus.crm.mapper.FurnitureItemMapper;
import wh.plus.crm.model.EntityType;
import wh.plus.crm.model.furniture.FurnitureItem;
import wh.plus.crm.model.notification.NotificationType;
import wh.plus.crm.model.order.Order;
import wh.plus.crm.model.order.OrderItem;
import wh.plus.crm.model.order.OrderKind;
import wh.plus.crm.model.order.OrderStatus;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.supplier.Supplier;
import wh.plus.crm.repository.FurnitureItemRepository;
import wh.plus.crm.repository.OrderRepository;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.repository.SupplierRepository;
import wh.plus.crm.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FurnitureService {

    private final FurnitureItemRepository repository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final NotificationService notificationService;
    private final FurnitureItemMapper mapper;

    @Transactional(readOnly = true)
    public List<FurnitureItemDTO> list(Long projectId) {
        return repository.findAllByProject_IdOrderBySortOrderAscIdAsc(projectId)
                .stream().map(mapper::toDto).toList();
    }

    @Transactional
    public FurnitureItemDTO create(Long projectId, FurnitureItemDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        FurnitureItem item = new FurnitureItem();
        item.setProject(project);
        mapper.update(dto, item);
        applyResponsible(item, dto.getResponsibleUserId());
        if (item.getSortOrder() == null) item.setSortOrder(nextOrder(projectId));
        if (item.getStatus() == null) item.setStatus("Do ustalenia");
        if (item.getOrdered() == null) item.setOrdered(false);
        return mapper.toDto(repository.save(item));
    }

    @Transactional
    public FurnitureItemDTO update(Long id, FurnitureItemDTO dto) {
        FurnitureItem item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Furniture item not found: " + id));
        mapper.update(dto, item);
        applyResponsible(item, dto.getResponsibleUserId());
        return mapper.toDto(repository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** Wysyła wybrane pozycje mebli do zamówień (tworzy zamówienia, oznacza pozycje jako zamówione). */
    @Transactional
    public List<FurnitureItemDTO> sendToOrder(Long projectId, List<Long> ids) {
        for (Long id : ids) {
            FurnitureItem item = repository.findById(id).orElse(null);
            if (item == null || Boolean.TRUE.equals(item.getOrdered())) continue;
            orderRepository.save(buildOrder(item));
            item.setOrdered(true);
            item.setStatus("Zamówione");
            repository.save(item);

            // Powiadom osobę odpowiedzialną za pozycję.
            if (item.getResponsibleUser() != null) {
                notificationService.createImmediateNotification(
                        List.of(item.getResponsibleUser().getId()),
                        NotificationType.IMMEDIATE, false, false, false, null,
                        "Nowe zamówienie z pozycji mebli: " + item.getName(),
                        EntityType.PROJECT, projectId);
            }
        }
        return list(projectId);
    }

    private Order buildOrder(FurnitureItem item) {
        Order o = new Order();
        o.setProject(item.getProject());
        o.setName(item.getName());
        o.setType("Meble");
        o.setStageId(item.getStageId());
        o.setStatus(OrderStatus.DRAFT);
        o.setOrderKind(OrderKind.ORDER);
        o.setCurrency("PLN");
        // Dostawca strukturalnie (find-or-create po nazwie producenta) — nie tylko w notatce,
        // żeby dało się filtrować/raportować zamówienia per dostawca.
        o.setSupplier(resolveSupplier(item.getProducer()));
        Integer qty = item.getQuantity() != null ? item.getQuantity() : 1;
        BigDecimal unit = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        o.setNetValue(unit.multiply(BigDecimal.valueOf(qty)));
        // Pozycja zamówienia — zachowuje ilość i cenę jednostkową z rozpiski (inaczej ginęły).
        OrderItem oi = new OrderItem();
        oi.setOrder(o);
        oi.setName(item.getName());
        oi.setItemType(item.getColorCode() != null ? item.getColorCode() : "Meble");
        oi.setQuantity(qty);
        oi.setUnitPrice(unit);
        o.getItems().add(oi);

        StringBuilder notes = new StringBuilder();
        if (item.getLocation() != null) notes.append("Lokalizacja: ").append(item.getLocation()).append('\n');
        if (item.getColorCode() != null) notes.append("Kolor/materiał: ").append(item.getColorCode()).append('\n');
        if (item.getDimensions() != null) notes.append("Wymiary: ").append(item.getDimensions()).append('\n');
        if (item.getDetails() != null) notes.append(item.getDetails());
        o.setNotes(notes.toString().trim());
        return o;
    }

    /** Dopasowuje dostawcę po nazwie producenta lub zakłada nowy rekord w rejestrze dostawców. */
    private Supplier resolveSupplier(String producer) {
        if (producer == null || producer.isBlank()) return null;
        String name = producer.trim();
        return supplierRepository.findFirstByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Supplier s = new Supplier();
                    s.setName(name);
                    return supplierRepository.save(s);
                });
    }

    private void applyResponsible(FurnitureItem item, Long userId) {
        if (userId == null) {
            item.setResponsibleUser(null);
            return;
        }
        item.setResponsibleUser(userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)));
    }

    private Integer nextOrder(Long projectId) {
        return repository.findAllByProject_IdOrderBySortOrderAscIdAsc(projectId).stream()
                .map(FurnitureItem::getSortOrder).filter(Objects::nonNull)
                .max(Integer::compareTo).map(o -> o + 1).orElse(1);
    }
}
