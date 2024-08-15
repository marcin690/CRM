package wh.plus.crm.model.comment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.lead.Lead;

@Entity
@Table(name = "comment")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long globalId;
    private String content;

    @Enumerated(EnumType.STRING)
    private CommentType commentType;

    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    private boolean automatic;


    @ManyToOne
    @JoinColumn(name = "lead_id", nullable = true)
    private Lead lead;


//    @ManyToOne
//    @JoinColumn(name = "offer_id", nullable = true)
//    private Offer offer;
//
//    @ManyToOne
//    @JoinColumn(name = "project_id", nullable = true)
//    private Project project;
//
//    @ManyToOne
//    @JoinColumn(name = "contract_id", nullable = true)
//    private Contract contract;








}
