package wh.plus.crm.model.aivaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wh.plus.crm.model.Auditable;

import java.util.ArrayList;
import java.util.List;

/**
 * Zadanie wyceny AI. Jeden plik/rysunek = jedno zadanie = jedna konwersacja Dify
 * (agent wycenia jeden element na konwersację).
 */
@Entity
@Table(name = "valuation_job")
@Getter
@Setter
@NoArgsConstructor
public class ValuationJob extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kod agenta (np. "meble-wspolne"). */
    @Column(nullable = false)
    private String agentCode;

    /** Tytuł zadania — domyślnie nazwa pliku. */
    private String title;

    /** Opcjonalny opis/uwagi od użytkownika, dołączany do pierwszej wiadomości. */
    @Column(length = 2000)
    private String note;

    /** Klasa materiału przekazywana agentowi: ekonomiczna / standard / premium. */
    private String materialClass;

    /** Ilość sztuk (ważne dla rozkroju + ceny jednostkowej). */
    private Integer quantity;

    /** Publiczny URL rysunku w MinIO (podgląd/historia). */
    @Column(length = 1000)
    private String inputFileUrl;

    /** Identyfikator pliku po stronie Dify (upload_file_id). */
    private String difyFileId;

    /** Typ pliku dla Dify: "image" albo "document". */
    private String difyFileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ValuationStatus status = ValuationStatus.QUEUED;

    /** conversation_id z Dify — do kontynuacji czatu. */
    private String difyConversationId;

    /** Osobna konwersacja z agentem porównawczym (porównanie z ofertą dostawcy). */
    private String comparisonConversationId;

    /** Ostatni wynik porównania z ofertą dostawcy (markdown). */
    @Column(columnDefinition = "LONGTEXT")
    private String comparisonResult;

    /** Ostatnia odpowiedź agenta (markdown) — dla widoku listy/szczegółu. */
    @Column(columnDefinition = "LONGTEXT")
    private String resultMarkdown;

    /** Wyłuskany link do wygenerowanego pliku Excel. */
    @Column(length = 1000)
    private String excelUrl;

    @Column(length = 2000)
    private String errorMessage;

    /** Sumaryczny koszt wywołań (USD) z metadata.usage Dify. */
    private Double costUsd;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<ValuationMessage> messages = new ArrayList<>();

    public void addMessage(ValuationMessage m) {
        m.setJob(this);
        this.messages.add(m);
    }
}
