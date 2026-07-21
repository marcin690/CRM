package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.dto.order.OrderDTO;
import wh.plus.crm.dto.order.OrderItemDTO;
import wh.plus.crm.mapper.OrderMapper;
import wh.plus.crm.model.order.Order;
import wh.plus.crm.model.order.OrderItem;
import wh.plus.crm.model.project.Project;
import wh.plus.crm.model.supplier.Supplier;
import wh.plus.crm.repository.OrderRepository;
import wh.plus.crm.repository.ProjectRepository;
import wh.plus.crm.repository.SupplierRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProjectRepository projectRepository;
    private final SupplierRepository supplierRepository;
    private final OrderMapper mapper;

    @Transactional(readOnly = true)
    public List<OrderDTO> list(Long projectId) {
        return orderRepository.findAllByProject_IdOrderByIdDesc(projectId)
                .stream().map(mapper::toDto).toList();
    }

    /** Wszystkie zamówienia ze wszystkich projektów — globalna zakładka Zamówienia. */
    @Transactional(readOnly = true)
    public List<OrderDTO> listAll() {
        return orderRepository.findAllByOrderByIdDesc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public OrderDTO create(Long projectId, OrderDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Order order = new Order();
        order.setProject(project);
        mapper.update(dto, order);
        applySupplier(order, dto.getSupplierId());
        rebuildItems(order, dto.getItems());
        return mapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDTO update(Long orderId, OrderDTO dto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        mapper.update(dto, order);
        applySupplier(order, dto.getSupplierId());
        rebuildItems(order, dto.getItems());
        return mapper.toDto(orderRepository.save(order));
    }

    public void delete(Long orderId) {
        orderRepository.deleteById(orderId);
    }

    private void applySupplier(Order order, Long supplierId) {
        if (supplierId == null) {
            order.setSupplier(null);
            return;
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        order.setSupplier(supplier);
    }

    private void rebuildItems(Order order, List<OrderItemDTO> items) {
        order.getItems().clear();
        if (items == null) return;
        for (OrderItemDTO it : items) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setName(it.getName());
            item.setItemType(it.getItemType());
            item.setQuantity(it.getQuantity() != null ? it.getQuantity() : 1);
            item.setUnitPrice(it.getUnitPrice() != null ? it.getUnitPrice() : BigDecimal.ZERO);
            order.getItems().add(item);
        }
    }
}
