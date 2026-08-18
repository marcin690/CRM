package wh.plus.crm.service.aivaluation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.aivaluation.ValuationJob;
import wh.plus.crm.model.aivaluation.ValuationMessage;
import wh.plus.crm.model.aivaluation.ValuationStatus;
import wh.plus.crm.repository.ValuationJobRepository;
import wh.plus.crm.repository.ValuationMessageRepository;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wykonuje pojedyncze zadanie wyceny w tle (pula {@code valuationExecutor}).
 * Wywołuje agenta Dify, zapisuje odpowiedź i ustala status.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ValuationJobProcessor {

    private static final String DEFAULT_QUERY = "Wyceń element z załączonego rysunku technicznego.";
    private static final Pattern URL = Pattern.compile("https?://[^\\s)\\]]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICED_HINT =
            Pattern.compile("(?is).*(RAZEM|PLN\\s*netto|Wa[zż]no[sś][cć])\\b.*");

    private final ValuationJobRepository jobRepository;
    private final ValuationMessageRepository messageRepository;
    private final DifyClient difyClient;

    @Async("valuationExecutor")
    public void process(Long jobId) {
        ValuationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Zadanie wyceny {} nie istnieje", jobId);
            return;
        }

        job.setStatus(ValuationStatus.PROCESSING);
        jobRepository.save(job);

        StringBuilder qb = new StringBuilder();
        if (job.getMaterialClass() != null && !job.getMaterialClass().isBlank()) {
            qb.append("KLASA MATERIALU: ").append(job.getMaterialClass()).append("\n");
        }
        if (job.getQuantity() != null && job.getQuantity() > 0) {
            qb.append("ILOSC: ").append(job.getQuantity()).append(" szt.\n");
        }
        qb.append((job.getNote() != null && !job.getNote().isBlank()) ? job.getNote() : DEFAULT_QUERY);

        List<DifyClient.FileRef> files = job.getDifyFileId() == null ? List.of()
                : List.of(new DifyClient.FileRef(job.getDifyFileType(), job.getDifyFileId()));

        callAgent(job, qb.toString(), files);
    }

    /**
     * Kolejna tura rozmowy (kontynuacja czatu) — wykonywana w tle, bo pełna wycena
     * bywa dłuższa niż limity requestu HTTP.
     */
    @Async("valuationExecutor")
    public void processFollowUp(Long jobId, String query, List<DifyClient.FileRef> files) {
        ValuationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Zadanie wyceny {} nie istnieje", jobId);
            return;
        }
        callAgent(job, query, files);
    }

    /** Wspólne wywołanie agenta + zapis wyniku/statusu/błędu. */
    private void callAgent(ValuationJob job, String query, List<DifyClient.FileRef> files) {
        try {
            DifyClient.ChatResult result = difyClient.sendMessage(
                    job.getAgentCode(), query, files,
                    job.getDifyConversationId(), "crm-job-" + job.getId());

            applyAssistantReply(job, result);
            jobRepository.save(job);
            log.info("Wycena {} zakończona statusem {}", job.getId(), job.getStatus());
        } catch (Exception e) {
            log.error("Błąd wyceny {}: {}", job.getId(), e.getMessage(), e);
            job.setStatus(ValuationStatus.FAILED);
            job.setErrorMessage(truncate(e.getMessage(), 2000));
            jobRepository.save(job);
        }
    }

    /**
     * Zapisuje odpowiedź agenta jako wiadomość, aktualizuje pola zadania i status.
     * Współdzielone przez pierwsze wywołanie (kolejka) i kontynuację czatu.
     */
    void applyAssistantReply(ValuationJob job, DifyClient.ChatResult result) {
        String answer = result.answer() == null ? "" : result.answer();

        ValuationMessage msg = new ValuationMessage();
        msg.setJob(job);
        msg.setRole(ValuationMessage.Role.ASSISTANT);
        msg.setContent(answer);
        msg.setDifyMessageId(result.messageId());
        messageRepository.save(msg);

        if (result.conversationId() != null && !result.conversationId().isBlank()) {
            job.setDifyConversationId(result.conversationId());
        }
        job.setResultMarkdown(answer);

        String excelUrl = extractExcelUrl(answer);
        if (excelUrl != null) {
            job.setExcelUrl(excelUrl);
        }
        job.setStatus((excelUrl != null || PRICED_HINT.matcher(answer).matches())
                ? ValuationStatus.PRICED : ValuationStatus.ACTION_REQUIRED);

        if (result.costUsd() != null) {
            double prev = job.getCostUsd() == null ? 0.0 : job.getCostUsd();
            job.setCostUsd(prev + result.costUsd());
        }
    }

    /** Zwraca pierwszy link wyglądający na pobranie Excela (xlsx / download / generate / flask). */
    static String extractExcelUrl(String text) {
        if (text == null) return null;
        Matcher m = URL.matcher(text);
        while (m.find()) {
            String url = m.group().replaceAll("[.,;]+$", "");
            String low = url.toLowerCase();
            if (low.contains("xlsx") || low.contains("/download") || low.contains("/generate")
                    || low.contains("flask")) {
                return url;
            }
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
