package wh.plus.crm.dto.project;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectStageDTO {
    private Long id;
    private Long projectId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer sortOrder;
    private Long responsibleUserId;
    private String responsibleUserName;
    private String notes;
    private LocalDateTime closedAt;
    private String closedComment;
    private List<StageTaskDTO> tasks = new ArrayList<>();
}
