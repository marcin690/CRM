package wh.plus.crm.dto.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientSummaryDTO {

    private Long id;
    private String clientFullName;
    private String clientBusinessName;


}
