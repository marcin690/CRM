package wh.plus.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.model.comment.CommentType;
import wh.plus.crm.model.comment.EntityType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private String clientId;
    private String content;
    private CommentType commentType;
    private EntityType entityType;


    private LocalDateTime timestamp;
    private boolean automatic;

    // Pole dla relacji Lead
    private Long leadId;
    private Long offerId;
    private Long projectId;
    private Long contractId;

    private String createdBy;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime creationDate;

}
