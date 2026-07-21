package wh.plus.crm.dto.sharepoint;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectSharePointFileDTO {
    private Long id;
    private String itemId;
    private String driveId;
    private String name;
    private String webUrl;
    private String mimeType;
    private Long size;
    private String category;
    private String createdBy;
    private LocalDateTime creationDate;
}
