package wh.plus.crm.dto.invoice;

import lombok.Data;
import wh.plus.crm.model.invoice.FakturowniaRole;

@Data
public class ProjectFakturowniaBindingDTO {
    private Long id;
    private Long projectId;
    private Long accountId;
    private String accountLabel;
    private Long companyId;
    private FakturowniaRole role;
    private String fakturowniaCategoryName;
    private Long stageId;
}
