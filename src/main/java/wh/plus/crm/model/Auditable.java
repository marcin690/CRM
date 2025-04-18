package wh.plus.crm.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Auditable<T> {

    @CreatedBy
    protected T createdBy;

//    @CreatedDate
    @Column(name = "creation_date", columnDefinition = "TIMESTAMP")
    protected LocalDateTime creationDate;

    @LastModifiedBy
    protected T lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date", columnDefinition = "TIMESTAMP")
    protected LocalDateTime lastModifiedDate;

    protected String clientGlobalId;

    @Transient
    private boolean skipAudit = false;

    @PrePersist
    public void prePersist() {

        if (!skipAudit && creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }


}
