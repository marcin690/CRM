package wh.plus.crm.dto.project;

import lombok.Data;
import wh.plus.crm.model.CommentSentiment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConstructionLogEntryDTO {
    private Long id;
    private Long projectId;
    private Long projectStageId;
    private String projectStageName;
    private LocalDate entryDate;
    private String title;
    private String scope;
    private String comments;
    private CommentSentiment commentSentiment;
    private List<String> attachmentUrls = new ArrayList<>();
    private List<ConstructionLogCommentDTO> replies = new ArrayList<>();
    private String createdBy;
    private LocalDateTime creationDate;
}
