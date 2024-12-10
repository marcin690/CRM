package wh.plus.crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.project.Project;

import java.time.LocalDateTime;


@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Event extends Auditable<String>  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column
    private LocalDateTime date;

    @Column
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column
    private EntityType entityType;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;



}
