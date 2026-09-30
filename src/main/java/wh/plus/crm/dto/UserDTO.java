package wh.plus.crm.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String password;   // tylko wejściowo (tworzenie usera); nigdy nie zwracane
    private String email;
    private String fullname;
    private Long phone;
    private String avatar;

    // RBAC / zespoły / uprawnienia (zarządzane w zakładce Users przez admina)
    private Set<String> roles;          // nazwy ról, np. ["ADMIN","MANAGER"]
    private Long teamId;
    private String teamName;
    private Boolean isSalesRepresentative;
    private Set<String> permissions;    // np. ["REPORTS"]
    private Boolean blocked;            // true = zablokowane logowanie
}
