package wh.plus.crm.model.offer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@EntityListeners(AuditingEntityListener.class)
@Data
@EnableJpaAuditing
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    @JsonIgnoreProperties({"username", "password", "email", "fullname", "phone", "roles", "leads", "assignedLeads", "enabled", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
    private User createdBy;

    private String name, desctiption;

    @CreatedDate
    private LocalDateTime cretedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private Currency currency;









}
