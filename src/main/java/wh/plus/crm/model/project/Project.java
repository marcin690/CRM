package wh.plus.crm.model.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.ContactInfoList;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.common.HasClientId;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Project extends Auditable<String> implements HasClientId {
    @Id
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "client")
    private Client client;

    private Long roomQuantity, projectNetValue;

    //Czy jest etapowy
    private boolean isStage;

    @ElementCollection
    @CollectionTable(name = "project_contact_info", joinColumns = @JoinColumn(name = "project"))
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<ContactInfoList> contactInfoListList;



    private String fakturowniaCategory;




}
