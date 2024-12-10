package wh.plus.crm.dto;
import lombok.Data;
import wh.plus.crm.dto.client.ClientSummaryDTO;

import wh.plus.crm.model.EntityType;


import java.time.LocalDateTime;
@Data
public class EventDTO {


    private Long id;
    private LocalDateTime date;
    private String comment;
    private EntityType entityType;
    private Long clientId;
    private Long projectId;



}
