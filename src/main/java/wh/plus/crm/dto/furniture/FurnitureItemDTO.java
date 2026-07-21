package wh.plus.crm.dto.furniture;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FurnitureItemDTO {
    private Long id;
    private Long projectId;
    private Long stageId;
    private String name;
    private String location;
    private Integer quantity;
    private String colorCode;
    private String producer;
    private String dimensions;
    private String details;
    private Long responsibleUserId;
    private String responsibleUserName;
    private String status;
    private String comment;
    private BigDecimal unitPrice;
    private Integer sortOrder;
    private Boolean ordered;
}
