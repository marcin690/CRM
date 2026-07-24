package wh.plus.crm.model.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.CommentSentiment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pojedynczy wpis w dzienniku budowy projektu (notatka z datą, autorem, sentymentem
 * i opcjonalnymi załącznikami w Minio).
 */
@Entity
@Table(name = "construction_log_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConstructionLogEntry extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_stage_id")
    private ProjectStage projectStage;

    private LocalDate entryDate;

    private String title;

    @Column(length = 1000)
    private String scope;

    @Column(length = 4000)
    private String comments;

    @Enumerated(EnumType.STRING)
    private CommentSentiment commentSentiment;

    @ElementCollection
    @CollectionTable(name = "construction_log_attachment",
            joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "url", length = 1024)
    private List<String> attachmentUrls = new ArrayList<>();

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("creationDate ASC")
    private List<ConstructionLogComment> replies = new ArrayList<>();
}
