package wh.plus.crm.dto.sharepoint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Element listingu SharePoint (plik lub folder) zwracany z Graph do wyboru w UI. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharePointItemDTO {
    private String id;
    private String name;
    private String webUrl;
    private boolean folder;
    private String mimeType;
    private Long size;
}
