package wh.plus.crm.model.contactInfo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import wh.plus.crm.model.Auditable;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_info")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor

public class ContactInfo extends Auditable<String> {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long globalId;

    @Version
    private int version;


    private String clientFullName, clientBusinessName, clientAdress, clientCity, clientState, clientZip, clientCountry, clientEmail;

    private Long clientPhone, vatNumber;

    private boolean isClient;


}
