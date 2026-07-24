package wh.plus.crm.model.sharepoint;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;
import wh.plus.crm.model.project.Project;

/**
 * Referencja (link) do pliku w SharePoint przypiętego do projektu, z kategorią.
 * Trzymamy tylko metadane i URL — pliku NIE kopiujemy ani nie modyfikujemy w SharePoint.
 */
@Entity
@Table(name = "project_sharepoint_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSharePointFile extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Graph driveItem id. */
    @Column(nullable = false, length = 512)
    private String itemId;

    private String driveId;

    private String name;

    @Column(length = 2048)
    private String webUrl;

    private String mimeType;

    private Long size;

    /** Kategoria pliku w kontekście projektu (Umowy / Rysunki / Zdjęcia / ...). */
    private String category;
}
