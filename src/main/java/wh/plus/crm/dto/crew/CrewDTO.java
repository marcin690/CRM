package wh.plus.crm.dto.crew;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CrewDTO {
    private Long id;
    private String name;
    private String foreman;
    private String email;
    private String phone;
    private String color;
    private boolean active = true;
    private List<CrewMemberDTO> members = new ArrayList<>();
}
