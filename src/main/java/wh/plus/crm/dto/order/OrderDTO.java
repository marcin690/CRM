package wh.plus.crm.dto.order;

import lombok.Data;
import wh.plus.crm.model.order.OrderKind;
import wh.plus.crm.model.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private String projectTeamName;
    private Long supplierId;
    private String supplierName;
    private String name;
    private String type;
    private OrderKind orderKind;
    private OrderStatus status;
    private String currency;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private BigDecimal netValue;
    private Long stageId;
    private String sharepointUrl;
    private String sharepointName;
    private String notes;
    private List<OrderItemDTO> items = new ArrayList<>();
    private String createdBy;
    private LocalDateTime creationDate;
}
