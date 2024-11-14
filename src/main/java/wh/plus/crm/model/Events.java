package wh.plus.crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;


@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Events extends Auditable<String> {

    @Column
    private LocalDateTime date;

    @Column
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column
    private EntityType entityType;


}
