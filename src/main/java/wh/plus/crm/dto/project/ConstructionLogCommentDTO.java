package wh.plus.crm.dto.project;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConstructionLogCommentDTO {
    private Long id;
    private String content;
    private String createdBy;
    private LocalDateTime creationDate;
}
