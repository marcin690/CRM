package wh.plus.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.dto.client.ClientSummaryDTO;
import wh.plus.crm.dto.project.ProjectSummaryDTO;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String comment;
    private ProjectSummaryDTO project;
    private ClientSummaryDTO client;

}
