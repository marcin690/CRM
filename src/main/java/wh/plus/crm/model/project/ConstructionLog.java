package wh.plus.crm.model.project;
//Dziennik budowy

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.CommentSentiment;

@Embeddable
public class ConstructionLog extends Auditable<String> {

    String title, scope, comments;

    @Enumerated(EnumType.STRING)
    private CommentSentiment commentSentiment;




}
