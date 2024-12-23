package wh.plus.crm.model.comment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.CommentSentiment;
import wh.plus.crm.model.EntityType;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.project.Project;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String content;

    @Enumerated(EnumType.STRING)
    private CommentSentiment commentSentiment;

    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    private boolean automatic;

    private LocalDateTime eventDate;


    @ManyToOne
    @JoinColumn(name = "lead_id", nullable = true)
    private Lead lead;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "offer_id", nullable = true)
    private Offer offer;



    // encje do uzupełmnienia
    private Long projectStage;
    private Long production;


    public void setClientFromLead(String clientId) {
        this.clientGlobalId = clientId;
    }


}
