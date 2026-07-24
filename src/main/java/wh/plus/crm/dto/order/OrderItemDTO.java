package wh.plus.crm.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long id;
    private String name;
    private String itemType;
    private Integer quantity;
    private BigDecimal unitPrice;
}
