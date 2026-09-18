package wh.plus.crm.service.aichat;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import wh.plus.crm.config.DifyConfig.DifyProperties;
import wh.plus.crm.dto.aichat.AiChatDtos.*;
import wh.plus.crm.dto.aichat.UserUsageRow;
import wh.plus.crm.model.aichat.AiChatApp;
import wh.plus.crm.model.aichat.AiConversation;
import wh.plus.crm.model.aichat.AiUsage;
import wh.plus.crm.repository.AiConversationRepository;
import wh.plus.crm.repository.AiUsageRepository;
import wh.plus.crm.service.aivaluation.DifyClient;
import wh.plus.crm.service.aivaluation.DifyClient.FileRef;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logika modułu Asystent AI: izolacja (własność rozmów), zapis zużycia, historia,
 * pliki i limity. Streaming SSE realizuje kontroler przy pomocy {@link DifyChatStreamer};
 * ten serwis dostarcza operacje pomocnicze i trwałość.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatService {

    private final AiConversationRepository conversationRepository;
    private final AiUsageRepository usageRepository;
    private final DifyChatStreamer streamer;
    private final DifyClient difyClient;   // reużycie uploadu pliku i feedbacku
    private final DifyProperties props;

    private static final int MAX_QUERY_LEN = 20_000;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int RATE_LIMIT_PER_MIN = 20;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    /** Właściciele wgranych plików: difyFileId -> (userId, expiryEpochMs). TTL 1h. */
    private final Map<String, long[]> fileOwners = new ConcurrentHashMap<>();
    /** Aktywny streaming per użytkownik (max 1). */
    private final Set<Long> activeStreams = ConcurrentHashMap.newKeySet();
    /** Sliding-window rate limit per użytkownik. */
    private final Map<Long, Deque<Long>> rateBuckets = new ConcurrentHashMap<>();
    /** Cache /parameters per czat (10 min). */
    private final Map<AiChatApp, long[]> paramsCacheTs = new ConcurrentHashMap<>();
    private final Map<AiChatApp, AiAppDto> paramsCache = new ConcurrentHashMap<>();

    // ===== Tożsamość Dify (zawsze z sesji, nigdy z requestu) =====

    public String difyUser(Long userId) {
        return props.getUserPrefix() + ":" + userId;
    }

    // ===== Lista czatów z opisami =====

    public List<AiAppDto> listApps(Long userId) {
        List<AiAppDto> out = new ArrayList<>();
        for (AiChatApp app : AiChatApp.values()) {
            out.add(appDto(app, userId));
        }
        return out;
    }

    private AiAppDto appDto(AiChatApp app, Long userId) {
        long[] ts = paramsCacheTs.get(app);
        if (ts != null && ts[0] > System.currentTimeMillis() && paramsCache.containsKey(app)) {
            return paramsCache.get(app);
        }
        String opening = null;
        List<String> suggested = List.of();
        try {
            JsonNode p = streamer.getParameters(app.getAgentCode(), difyUser(userId));
            opening = p.path("opening_statement").asText(null);
            JsonNode sq = p.path("suggested_questions");
            if (sq.isArray()) {
                List<String> tmp = new ArrayList<>();
                sq.forEach(n -> tmp.add(n.asText()));
                suggested = tmp;
            }
        } catch (Exception e) {
            log.debug("AI chat: /parameters dla {} niedostępne: {}", app, e.getMessage());
        }
        AiAppDto dto = new AiAppDto(app.name(), app.getLabel(), app.getDescription(), opening, suggested);
        paramsCache.put(app, dto);
        paramsCacheTs.put(app, new long[]{ System.currentTimeMillis() + 10 * 60_000 });
        return dto;
    }

    // ===== Lista rozmów (zawsze zawężona do właściciela) =====

    public List<AiConversationDto> listConversations(Long userId, String appCode) {
        List<AiConversation> rows;
        AiChatApp app = AiChatApp.fromCode(appCode);
        if (app != null) {
            rows = conversationRepository.findByOwnerUserIdAndAppAndDeletedAtIsNullOrderByUpdatedAtDesc(userId, app);
        } else {
            rows = conversationRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId);
        }
        return rows.stream().map(this::toDto).toList();
    }

    private AiConversationDto toDto(AiConversation c) {
        return new AiConversationDto(c.getId(), c.getApp().name(), c.getApp().getLabel(), c.getTitle(),
                c.getPoziom(), c.getUpdatedAt());
    }

    /** Pobranie rozmowy z weryfikacją właściciela. 404 dla cudzej/usuniętej/nieistniejącej. */
    public AiConversation getOwned(Long conversationId, Long userId) {
        return conversationRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono rozmowy"));
    }

    // ===== Przygotowanie rozmowy do czatu =====

    @Transactional
    public AiConversation prepareForChat(Long userId, ChatRequest req) {
        if (req.query() == null || req.query().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pusta wiadomość");
        }
        if (req.query().length() > MAX_QUERY_LEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wiadomość jest za długa");
        }
        if (req.conversationId() != null) {
            return getOwned(req.conversationId(), userId); // poziom/app z zapisu, req ignorujemy
        }
        AiChatApp app = AiChatApp.fromCode(req.app());
        if (app == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany czat");
        }
        AiConversation c = new AiConversation();
        c.setOwnerUserId(userId);
        c.setApp(app);
        c.setPoziom(AiChatApp.normPoziom(req.poziom()));
        c.setTitle(req.query().length() > 60 ? req.query().substring(0, 60) : req.query());
        return conversationRepository.save(c);
    }

    @Transactional
    public void onStreamStarted(Long conversationId, Long ownerUserId, String difyConversationId, String taskId) {
        // Owner-scoped reload (defense-in-depth) — nigdy nie piszemy do cudzej rozmowy.
        conversationRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(conversationId, ownerUserId).ifPresent(c -> {
            if (c.getDifyConversationId() == null && difyConversationId != null) {
                c.setDifyConversationId(difyConversationId);
            }
            c.setLastTaskId(taskId);
            conversationRepository.save(c);
        });
    }

    @Transactional
    public void saveUsageAndTouch(AiConversation conv, Long userId, String userEmail,
                                  String messageId, DifyChatStreamer.UsageInfo usage) {
        // Jawny bump updated_at (owner-scoped) — sam save() niezmienionej encji nie wygeneruje UPDATE.
        conversationRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(conv.getId(), userId).ifPresent(c -> {
            c.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(c);
        });
        if (messageId == null || messageId.isBlank() || usageRepository.existsByDifyMessageId(messageId)) {
            return;
        }
        AiUsage u = new AiUsage();
        u.setUserId(userId);
        u.setUserEmail(userEmail != null ? userEmail.toLowerCase() : null);
        u.setConversationId(conv.getId());
        u.setApp(conv.getApp());
        u.setModel(conv.getApp().modelFor(conv.getPoziom()));
        u.setDifyMessageId(messageId);
        u.setPromptTokens(usage.promptTokens());
        u.setCompletionTokens(usage.completionTokens());
        BigDecimal price = parsePrice(usage.totalPrice());
        u.setDifyTotalPrice(price);
        u.setCostUsd(price);
        u.setLatencyMs((int) Math.round(usage.latencySeconds() * 1000));
        usageRepository.save(u);
    }

    private BigDecimal parsePrice(String s) {
        try { return (s == null || s.isBlank()) ? BigDecimal.ZERO : new BigDecimal(s); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    // ===== Historia =====

    public AiMessagesPage getMessages(Long conversationId, Long userId, String firstId) {
        AiConversation c = getOwned(conversationId, userId);
        if (c.getDifyConversationId() == null) {
            return new AiMessagesPage(List.of(), false);
        }
        try {
            JsonNode res = streamer.getMessages(c.getApp().getAgentCode(), c.getDifyConversationId(),
                    difyUser(userId), firstId, 20);
            List<AiMessageDto> msgs = new ArrayList<>();
            JsonNode data = res.path("data");
            if (data.isArray()) {
                for (JsonNode m : data) {
                    msgs.add(new AiMessageDto(
                            m.path("id").asText(""),
                            m.path("query").asText(""),
                            m.path("answer").asText(""),
                            null));
                }
            }
            return new AiMessagesPage(msgs, res.path("has_more").asBoolean(false));
        } catch (Exception e) {
            log.warn("AI chat: pobranie historii {} nie powiodło się: {}", conversationId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nie udało się pobrać historii");
        }
    }

    // ===== Zmiana tytułu / usunięcie =====

    @Transactional
    public AiConversationDto rename(Long conversationId, Long userId, String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tytuł nie może być pusty");
        }
        AiConversation c = getOwned(conversationId, userId);
        String trimmed = title.trim();
        if (c.getDifyConversationId() != null) {
            try { streamer.renameConversation(c.getApp().getAgentCode(), c.getDifyConversationId(), trimmed, difyUser(userId)); }
            catch (Exception e) { log.warn("AI chat: rename w Dify nie powiódł się: {}", e.getMessage()); }
        }
        c.setTitle(trimmed);
        conversationRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public void delete(Long conversationId, Long userId) {
        AiConversation c = getOwned(conversationId, userId);
        if (c.getDifyConversationId() != null) {
            try { streamer.deleteConversation(c.getApp().getAgentCode(), c.getDifyConversationId(), difyUser(userId)); }
            catch (Exception e) { log.warn("AI chat: delete w Dify nie powiódł się (oznaczam usunięte): {}", e.getMessage()); }
        }
        c.setDeletedAt(LocalDateTime.now());
        conversationRepository.save(c);
    }

    // ===== Pliki (obrazy) =====

    public FileUploadedDto uploadImage(Long userId, String appCode, byte[] bytes, String filename, String contentType) {
        AiChatApp app = AiChatApp.fromCode(appCode);
        if (app == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznany czat");
        if (bytes == null || bytes.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pusty plik");
        if (bytes.length > MAX_FILE_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plik za duży (max 10 MB)");
        String sniffed = sniffImageType(bytes);
        if (sniffed == null || (contentType != null && !ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_IMAGE_TYPES.contains(sniffed))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dozwolone są tylko obrazy PNG, JPG, WEBP, GIF");
        }
        String id = difyClient.uploadFile(app.getAgentCode(), bytes, filename, sniffed, difyUser(userId));
        fileOwners.put(id, new long[]{ userId, System.currentTimeMillis() + 3_600_000 });
        return new FileUploadedDto(id);
    }

    /** Buduje FileRef tylko dla plików należących do usera; obcy/nieznany -> 400. */
    public List<FileRef> resolveOwnedFiles(Long userId, List<String> fileIds) {
        List<FileRef> refs = new ArrayList<>();
        if (fileIds == null) return refs;
        long now = System.currentTimeMillis();
        for (String id : fileIds) {
            long[] owner = fileOwners.get(id);
            if (owner == null || owner[0] != userId || owner[1] < now) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieprawidłowy plik");
            }
            refs.add(new FileRef("image", id));
        }
        return refs;
    }

    /** Rozpoznanie typu obrazu po magic bytes (nie po rozszerzeniu). */
    private String sniffImageType(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "image/gif";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }

    // ===== Feedback =====

    public void feedback(Long conversationId, Long userId, String messageId, String rating, String content) {
        AiConversation c = getOwned(conversationId, userId);
        if (messageId == null || messageId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak wiadomości");
        }
        // Izolacja: messageId musi należeć do TEJ rozmowy (ai_usage zapisuje difyMessageId + conversationId).
        // Bez tego można byłoby wysłać feedback do cudzej wiadomości podając jej id.
        if (!usageRepository.existsByConversationIdAndDifyMessageId(conversationId, messageId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono wiadomości");
        }
        try { difyClient.sendFeedback(c.getApp().getAgentCode(), messageId, rating, content, difyUser(userId)); }
        catch (Exception e) { log.warn("AI chat: feedback nie powiódł się: {}", e.getMessage()); }
    }

    // ===== Stop =====

    public void stop(Long conversationId, Long userId) {
        AiConversation c = getOwned(conversationId, userId);
        if (c.getLastTaskId() == null) return;
        try { streamer.stopGeneration(c.getApp().getAgentCode(), c.getLastTaskId(), difyUser(userId)); }
        catch (Exception e) { log.warn("AI chat: stop nie powiódł się: {}", e.getMessage()); }
    }

    // ===== Limity i współbieżność =====

    public void checkRateLimit(Long userId) {
        long now = System.currentTimeMillis();
        Deque<Long> q = rateBuckets.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < now - 60_000) q.pollFirst();
            if (q.size() >= RATE_LIMIT_PER_MIN) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Za dużo wiadomości, poczekaj chwilę");
            }
            q.addLast(now);
        }
    }

    public void acquireStreamSlot(Long userId) {
        if (!activeStreams.add(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Poczekaj na zakończenie poprzedniej odpowiedzi");
        }
    }

    public void releaseStreamSlot(Long userId) {
        activeStreams.remove(userId);
    }

    // ===== Raport admina =====

    public List<UsageReportRow> usageReport(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<UserUsageRow> rows = usageRepository.aggregateSince(since);
        List<UsageReportRow> out = new ArrayList<>();
        for (UserUsageRow r : rows) {
            out.add(new UsageReportRow(
                    r.getUserId(), r.getUserEmail(), r.getConversations(), r.getMessages(),
                    r.getPromptTokens(), r.getCompletionTokens(),
                    r.getCostUsd() != null ? r.getCostUsd().doubleValue() : 0.0));
        }
        return out;
    }
}
