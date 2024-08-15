package wh.plus.crm.dto;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfoDTO {
    private Long id;
    private String fullName;
    private String clientBusinessName;
    private String clientAdress;
    private String clientCity;
    private String clientState;
    private String clientZip;
    private String clientCountry;
    private String clientEmail;
    private Long clientPhone;
    private Long vatNumber;
    private boolean isClient;
}
