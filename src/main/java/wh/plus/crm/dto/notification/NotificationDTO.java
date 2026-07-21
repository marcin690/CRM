package wh.plus.crm.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private String content;
    private boolean open;
    private LocalDateTime creationDate;
    private String link;
}
