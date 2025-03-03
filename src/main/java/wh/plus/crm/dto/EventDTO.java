package wh.plus.crm.dto;
import lombok.Data;
import wh.plus.crm.dto.client.ClientSummaryDTO;

import wh.plus.crm.model.EntityType;
import wh.plus.crm.model.notification.CycleType;


import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventDTO {


    private Long id;
    private LocalDateTime date;
    private String comment;
    private EntityType entityType;
    private Long clientId;
    private Long projectId;

    private LocalDateTime cycleEndDate;

    private Boolean cyclic;       // true, jeśli powiadomienie ma być cykliczne
    private CycleType cycleType;  // typ cykliczności: DAILY, WEEKLY, itp.
    private Boolean sendEmail;    // czy wysłać e-mail
    private Boolean sendSms;      // czy wysłać SMS
    private List<Long> recipientIds;
    private LocalDateTime nextNotificationTime;




}
