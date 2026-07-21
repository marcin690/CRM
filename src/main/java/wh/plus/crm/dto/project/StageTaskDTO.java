package wh.plus.crm.dto.project;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StageTaskDTO {
    private Long id;
    private String title;
    private LocalDate dueDate;
    private String status;
    private boolean done;
    private Integer sortOrder;
    private String type;
    private String montageMode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long responsibleUserId;
    private String responsibleUserName;
    private boolean privateOnly;
    private String details;
    private String createdBy;
}
