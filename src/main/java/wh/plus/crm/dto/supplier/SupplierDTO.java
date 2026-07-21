package wh.plus.crm.dto.supplier;

import lombok.Data;

@Data
public class SupplierDTO {
    private Long id;
    private String name;
    private String nip;
    private String email;
    private String phone;
    private String contactPerson;
    private String address;
    private String notes;
    private boolean active = true;
}
