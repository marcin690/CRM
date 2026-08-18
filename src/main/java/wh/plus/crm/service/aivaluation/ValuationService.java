package wh.plus.crm.service.aivaluation;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import wh.plus.crm.config.DifyConfig;
import wh.plus.crm.dto.aivaluation.AiAgentDto;
import wh.plus.crm.dto.aivaluation.ValuationJobDto;
import wh.plus.crm.dto.aivaluation.ValuationMessageDto;
import wh.plus.crm.model.aivaluation.AiAgent;
import wh.plus.crm.model.aivaluation.ValuationJob;
import wh.plus.crm.model.aivaluation.ValuationMessage;
import wh.plus.crm.model.aivaluation.ValuationStatus;
import wh.plus.crm.repository.AiAgentRepository;
import wh.plus.crm.repository.ValuationJobRepository;
import wh.plus.crm.repository.ValuationMessageRepository;
import wh.plus.crm.service.MinioUploadService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValuationService {

    private final ValuationJobRepository jobRepository;
    private final ValuationMessageRepository messageRepository;
    private final AiAgentRepository agentRepository;
    private final MinioUploadService minioUploadService;
    private final DifyClient difyClient;
    private final ValuationJobProcessor processor;
    private final DifyConfig.DifyProperties difyProperties;

    /** Kod agenta porównawczego (osobny agent Dify, nie w dropdownie wycen). */
    private static final String COMPARISON_AGENT = "porownanie-ofert";

    // --- Agenci ---

    /** Zwraca aktywnych agentów, dla których skonfigurowano klucz API (bez klucza = ukryty). */
    public List<AiAgentDto> listAgents() {
        return agentRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(a -> {
                    String key = difyProperties.getAgentKeys().get(a.getCode());
                    return key != null && !key.isBlank();
                })
                .map(a -> new AiAgentDto(a.getCode(), a.getName(), a.getDescription()))
                .toList();
    }

    // --- Feedback: przekazywany do Dify (widoczny w logach Dify jako ocena użytkownika) ---

    /**
     * Wysyła ocenę (rating "like"/"dislike") i/lub komentarz do Dify dla ostatniej
     * odpowiedzi agenta w tej wycenie. Nie zapisujemy tego lokalnie.
     */
    @Transactional(readOnly = true)
    public void submitFeedback(Long jobId, String rating, String comment) {
        ValuationJob job = findJob(jobId);
        if ((rating == null || rating.isBlank()) && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("Podaj ocenę lub komentarz");
        }
        String messageId = job.getMessages().stream()
                .filter(m -> m.getRole() == ValuationMessage.Role.ASSISTANT && m.getDifyMessageId() != null)
                .reduce((a, b) -> b) // ostatnia odpowiedź agenta
                .map(ValuationMessage::getDifyMessageId)
                .orElseThrow(() -> new IllegalStateException(
                        "Tej wyceny nie można ocenić — powstała przed włączeniem ocen (brak powiązania z Dify). "
                                + "Utwórz nową wycenę, aby przesłać ocenę."));

        difyClient.sendFeedback(job.getAgentCode(), messageId, rating, comment, "crm-job-" + job.getId());
    }

    // --- Tworzenie zadań ---

    /**
     * Tworzy po jednym zadaniu na każdy przesłany plik (1 plik = 1 pozycja = 1 konwersacja)
     * i wrzuca je do kolejki. Zwraca skróconą listę utworzonych zadań.
     */
    @Transactional
    public List<ValuationJobDto> createJobs(String agentCode, String note, String materialClass,
                                            Integer quantity, MultipartFile[] files) {
        AiAgent agent = agentRepository.findByCode(agentCode)
                .filter(AiAgent::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Nieznany lub nieaktywny agent: " + agentCode));

        boolean hasFiles = false;
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) { hasFiles = true; break; }
            }
        }
        boolean hasDesc = (note != null && !note.isBlank()) || (materialClass != null && !materialClass.isBlank());
        if (!hasFiles && !hasDesc) {
            throw new IllegalArgumentException("Dodaj plik (rysunek) lub opis wyceny");
        }

        List<ValuationJob> created = new ArrayList<>();
        List<Long> toEnqueue = new ArrayList<>();

        if (!hasFiles) {
            // Wycena z samego opisu (bez rysunku)
            ValuationJob job = new ValuationJob();
            job.setAgentCode(agent.getCode());
            job.setTitle("Wycena z opisu");
            job.setNote(note);
            job.setMaterialClass(materialClass);
            job.setQuantity(quantity);
            job.setStatus(ValuationStatus.QUEUED);
            job = jobRepository.save(job);
            saveMessage(job, ValuationMessage.Role.USER,
                    (note != null && !note.isBlank()) ? note : "Wyceń produkt wg podanych ustawień.");
            jobRepository.save(job);
            created.add(job);
            toEnqueue.add(job.getId());
        } else {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                ValuationJob job = new ValuationJob();
                job.setAgentCode(agent.getCode());
                job.setTitle(file.getOriginalFilename());
                job.setNote(note);
                job.setMaterialClass(materialClass);
                job.setQuantity(quantity);
                job.setStatus(ValuationStatus.QUEUED);
                job = jobRepository.save(job);

                String user = "crm-job-" + job.getId();
                try {
                    job.setInputFileUrl(minioUploadService.uploadFile(file));

                    String contentType = file.getContentType();
                    String difyType = (contentType != null && contentType.startsWith("image/")) ? "image" : "document";
                    String fileId = difyClient.uploadFile(agent.getCode(), file.getBytes(),
                            file.getOriginalFilename(), contentType, user);
                    job.setDifyFileId(fileId);
                    job.setDifyFileType(difyType);

                    saveMessage(job, ValuationMessage.Role.USER,
                            (note != null && !note.isBlank()) ? note
                                    : "Wyceń element z załączonego rysunku: " + file.getOriginalFilename());
                    jobRepository.save(job);
                } catch (IOException | RuntimeException e) {
                    log.error("Nie udało się przygotować zadania {}: {}", job.getId(), e.getMessage(), e);
                    job.setStatus(ValuationStatus.FAILED);
                    job.setErrorMessage("Przygotowanie pliku: " + e.getMessage());
                    jobRepository.save(job);
                    created.add(job);
                    continue;
                }

                created.add(job);
                toEnqueue.add(job.getId());
            }
        }

        // odpal kolejkę dopiero PO zatwierdzeniu transakcji — inaczej wątek w tle
        // mógłby nie zobaczyć zapisanych zadań (race @Async vs commit)
        enqueueAfterCommit(toEnqueue);

        return created.stream().map(this::toSummary).toList();
    }

    private void enqueueAfterCommit(List<Long> jobIds) {
        if (jobIds.isEmpty()) return;
        runAfterCommit(() -> jobIds.forEach(processor::process));
    }

    /** Uruchamia akcję dopiero po zatwierdzeniu bieżącej transakcji (albo od razu, gdy brak tx). */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    // --- Odczyt ---

    @Transactional(readOnly = true)
    public List<ValuationJobDto> listJobs() {
        return jobRepository.findAllByOrderByIdDesc().stream().map(this::toSummary).toList();
    }

    /** Liczba wycen w toku (kolejka lub w trakcie) — do globalnego wskaźnika w menu. */
    @Transactional(readOnly = true)
    public long activeCount() {
        return jobRepository.countByStatusIn(List.of(ValuationStatus.QUEUED, ValuationStatus.PROCESSING));
    }

    @Transactional(readOnly = true)
    public ValuationJobDto getJob(Long id) {
        return toDetail(findJob(id));
    }

    // --- Porównanie z ofertą dostawcy (agent porównawczy, synchronicznie) ---

    /**
     * Porównuje gotową wycenę z ofertą dostawcy (opis i/lub pliki). Można wołać wielokrotnie
     * (kolejne oferty dochodzą do tej samej konwersacji — porównanie 1:wiele).
     */
    @Transactional
    public ValuationJobDto compareWithOffer(Long id, String description, MultipartFile[] files) {
        ValuationJob job = findJob(id);
        if (job.getResultMarkdown() == null || job.getResultMarkdown().isBlank()) {
            throw new IllegalStateException("Najpierw musi powstać wycena, żeby porównać ją z ofertą");
        }
        boolean hasDesc = description != null && !description.isBlank();
        boolean hasFiles = false;
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) { hasFiles = true; break; }
            }
        }
        if (!hasDesc && !hasFiles) {
            throw new IllegalArgumentException("Podaj opis oferty i/lub załącz plik");
        }

        String user = "crm-cmp-" + job.getId();
        List<DifyClient.FileRef> refs = new ArrayList<>();
        try {
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) continue;
                    String ct = file.getContentType();
                    String type = (ct != null && ct.startsWith("image/")) ? "image" : "document";
                    String fid = difyClient.uploadFile(COMPARISON_AGENT, file.getBytes(),
                            file.getOriginalFilename(), ct, user);
                    refs.add(new DifyClient.FileRef(type, fid));
                }
            }
            String query = "NASZA WYCENA:\n" + job.getResultMarkdown()
                    + "\n\nOFERTA DOSTAWCY:\n" + (hasDesc ? description : "(w załączonym pliku)");
            DifyClient.ChatResult result = difyClient.sendMessage(
                    COMPARISON_AGENT, query, refs, job.getComparisonConversationId(), user);
            if (result.conversationId() != null && !result.conversationId().isBlank()) {
                job.setComparisonConversationId(result.conversationId());
            }
            job.setComparisonResult(result.answer());
            jobRepository.save(job);
        } catch (IOException | RuntimeException e) {
            log.error("Błąd porównania {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Nie udało się wykonać porównania: " + e.getMessage(), e);
        }
        return toDetail(job);
    }

    // --- Kontynuacja czatu (synchronicznie) ---

    /**
     * Wysyła kolejną wiadomość w istniejącej konwersacji Dify i zwraca zaktualizowany szczegół.
     */
    @Transactional
    public ValuationJobDto continueChat(Long id, String query, MultipartFile[] files) {
        ValuationJob job = findJob(id);
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Pusta wiadomość");
        }
        if (job.getStatus() == ValuationStatus.PROCESSING) {
            throw new IllegalStateException("Poprzednia wiadomość jest jeszcze przetwarzana — poczekaj na odpowiedź agenta");
        }

        String user = "crm-job-" + job.getId();
        List<DifyClient.FileRef> refs = new ArrayList<>();
        try {
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) continue;
                    String contentType = file.getContentType();
                    String difyType = (contentType != null && contentType.startsWith("image/")) ? "image" : "document";
                    String fileId = difyClient.uploadFile(job.getAgentCode(), file.getBytes(),
                            file.getOriginalFilename(), contentType, user);
                    refs.add(new DifyClient.FileRef(difyType, fileId));
                }
            }
        } catch (IOException | RuntimeException e) {
            log.error("Błąd uploadu pliku do czatu zadania {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Nie udało się wysłać pliku do agenta: " + e.getMessage(), e);
        }

        saveMessage(job, ValuationMessage.Role.USER, query);
        job.setStatus(ValuationStatus.PROCESSING);
        jobRepository.save(job);

        // wywołanie agenta w tle — pełna wycena bywa dłuższa niż limity requestu HTTP
        List<DifyClient.FileRef> finalRefs = refs;
        runAfterCommit(() -> processor.processFollowUp(id, query, finalRefs));

        return toDetail(job);
    }

    // --- Pomocnicze ---

    private ValuationJob findJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Zadanie wyceny nie istnieje: " + id));
    }

    private void saveMessage(ValuationJob job, ValuationMessage.Role role, String content) {
        ValuationMessage m = new ValuationMessage();
        m.setJob(job);
        m.setRole(role);
        m.setContent(content);
        messageRepository.save(m);
    }

    private String agentName(String code) {
        return agentRepository.findByCode(code).map(AiAgent::getName).orElse(code);
    }

    private ValuationJobDto toSummary(ValuationJob j) {
        return new ValuationJobDto(j.getId(), j.getAgentCode(), agentName(j.getAgentCode()),
                j.getTitle(), j.getNote(), j.getStatus().name(), j.getInputFileUrl(), j.getExcelUrl(),
                null, j.getErrorMessage(), j.getDifyConversationId(), j.getCostUsd(), j.getCreatedBy(),
                j.getCreationDate(), null, null);
    }

    private ValuationJobDto toDetail(ValuationJob j) {
        List<ValuationMessageDto> msgs = j.getMessages().stream()
                .map(m -> new ValuationMessageDto(m.getId(), m.getRole().name(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new ValuationJobDto(j.getId(), j.getAgentCode(), agentName(j.getAgentCode()),
                j.getTitle(), j.getNote(), j.getStatus().name(), j.getInputFileUrl(), j.getExcelUrl(),
                j.getResultMarkdown(), j.getErrorMessage(), j.getDifyConversationId(), j.getCostUsd(),
                j.getCreatedBy(), j.getCreationDate(), j.getComparisonResult(), msgs);
    }
}
