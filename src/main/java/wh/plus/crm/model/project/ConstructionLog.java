package wh.plus.crm.model.project;
//Dziennik budowy

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.envers.Audited;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.CommentSentiment;
import wh.plus.crm.model.common.HasClientId;

import java.time.LocalDateTime;

@Embeddable
public class ConstructionLog extends Auditable<String> implements HasClientId {

    String title, scope, comments;

    @Enumerated(EnumType.STRING)
    private CommentSentiment commentSentiment;




}
