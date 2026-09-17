package wh.plus.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import wh.plus.crm.dto.aichat.AiChatDtos.*;
import wh.plus.crm.model.aichat.AiConversation;
import wh.plus.crm.model.user.User;
import wh.plus.crm.service.aichat.AiChatService;
import wh.plus.crm.service.aichat.DifyChatStreamer;
import wh.plus.crm.service.aivaluation.DifyClient.FileRef;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asystent AI — proxy czatów Dify z pełną izolacją (user zawsze z sesji, own-check → 404).
 * Streaming przez {@link SseEmitter}: backend pompuje SSE z Dify do przeglądarki,
 * nie ujawniając kluczy ani dify_conversation_id.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AiChatService service;
    private final DifyChatStreamer streamer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Field-injection z @Qualifier — pewne rozróżnienie od valuationExecutor (Lombok bywa zawodny przy @Qualifier w konstruktorze). */
    @Autowired
    @Qualifier("aiChatExecutor")
    private Executor executor;

    // ===== Meta =====

    @GetMapping("/apps")
    public List<AiAppDto> apps() {
        return service.listApps(currentUser().getId());
    }

    @GetMapping("/conversations")
    public List<AiConversationDto> conversations(@RequestParam(value = "app", required = false) String app) {
        return service.listConversations(currentUser().getId(), app);
    }

    @GetMapping("/conversations/{id}/messages")
    public AiMessagesPage messages(@PathVariable Long id,
                                   @RequestParam(value = "firstId", required = false) String firstId) {
        return service.getMessages(id, currentUser().getId(), firstId);
    }

    @PatchMapping("/conversations/{id}")
    public AiConversationDto rename(@PathVariable Long id, @RequestBody RenameRequest req) {
        return service.rename(id, currentUser().getId(), req.title());
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, currentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/conversations/{id}/feedback")
    public ResponseEntity<Void> feedback(@PathVariable Long id, @RequestBody FeedbackRequest req,
                                         @RequestParam(value = "messageId") String messageId) {
        service.feedback(id, currentUser().getId(), messageId, req.rating(), req.content());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/chat/{id}/stop")
    public ResponseEntity<Void> stop(@PathVariable Long id) {
        service.stop(id, currentUser().getId());
        return ResponseEntity.ok().build();
    }

    // ===== Upload obrazu =====

    @PostMapping("/files")
    public FileUploadedDto upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam("app") String app) {
        try {
            return service.uploadImage(currentUser().getId(), app, file.getBytes(),
                    file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Nie udało się odczytać pliku");
        }
    }

    // ===== Streaming czatu =====

    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatRequest req, HttpServletResponse response) {
        User user = currentUser();
        Long uid = user.getId();
        String userEmail = user.getEmail();

        // Walidacja/izolacja PRZED otwarciem strumienia (może rzucić 400/404/429).
        service.checkRateLimit(uid);
        AiConversation conv = service.prepareForChat(uid, req);
        List<FileRef> files = service.resolveOwnedFiles(uid, req.fileIds());
        service.acquireStreamSlot(uid); // 409 gdy user ma już aktywny stream

        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");

        // Timeout emittera (5,5 min) celowo > deadline watchdoga streamera (5 min):
        // to watchdog kończy pompę pierwszy, a slot zwalniany jest DOKŁADNIE RAZ w finally zadania.
        SseEmitter emitter = new SseEmitter(330_000L);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));   // NIE zwalniamy slotu tu (ślepe removeByUid zwolniłoby cudzy stream)
        emitter.onTimeout(() -> { cancelled.set(true); emitter.complete(); });
        emitter.onError(e -> cancelled.set(true));

        final String difyUser = service.difyUser(uid);
        final String query = req.query();

        try {
            executor.execute(() -> {
            try {
                streamer.stream(conv.getApp().getAgentCode(), query, files, conv.getDifyConversationId(), difyUser,
                        new DifyChatStreamer.Handler() {
                            @Override
                            public void onStart(String difyConversationId, String taskId) {
                                if (conv.getDifyConversationId() == null) conv.setDifyConversationId(difyConversationId);
                                conv.setLastTaskId(taskId);
                                service.onStreamStarted(conv.getId(), uid, difyConversationId, taskId);
                                send(emitter, Map.of("type", "start", "conversationId", conv.getId()));
                            }
                            @Override
                            public void onDelta(String text) {
                                send(emitter, Map.of("type", "delta", "text", text));
                            }
                            @Override
                            public void onEnd(String messageId, DifyChatStreamer.UsageInfo usage) {
                                // Najpierw domknij stream dla klienta, potem zapisz zużycie — awaria zapisu
                                // (np. wyścig na unikalnym message_id) nie może zamienić udanej odpowiedzi w 'error'.
                                send(emitter, Map.of("type", "end", "messageId", messageId == null ? "" : messageId));
                                try {
                                    service.saveUsageAndTouch(conv, uid, userEmail, messageId, usage);
                                } catch (Exception ex) {
                                    log.warn("AI chat: zapis zużycia nie powiódł się (odpowiedź dostarczona): {}", ex.getMessage());
                                }
                            }
                            @Override
                            public void onError(String friendlyMessage) {
                                send(emitter, Map.of("type", "error", "message", friendlyMessage));
                            }
                        }, cancelled::get);
            } catch (Exception e) {
                log.error("AI chat: nieoczekiwany błąd streamu: {}", e.getMessage());
                send(emitter, Map.of("type", "error", "message", "Asystent nie odpowiada, spróbuj za chwilę"));
            } finally {
                service.releaseStreamSlot(uid);
                try { emitter.complete(); } catch (Exception ignore) { /* już zamknięty */ }
            }
            });
        } catch (RejectedExecutionException rejected) {
            // Kolejka pełna — nie zostaw zawieszonego slotu ani wiszącego emittera.
            log.warn("AI chat: pula wątków przepełniona, odrzucono zadanie usera {}", uid);
            service.releaseStreamSlot(uid);
            send(emitter, Map.of("type", "error", "message", "Asystent jest przeciążony, spróbuj za chwilę"));
            emitter.complete();
        }

        return emitter;
    }

    // ===== Raport admina =====

    @GetMapping("/admin/usage")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UsageReportRow> usage(@RequestParam(value = "days", defaultValue = "30") int days) {
        int d = (days == 90 || days == 365) ? days : 30;
        return service.usageReport(d);
    }

    // ===== Pomocnicze =====

    private void send(SseEmitter emitter, Object payload) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            // Przeglądarka się rozłączyła — kończymy cicho.
        }
    }

    private User currentUser() {
        Object p = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        if (p instanceof User u) return u;
        throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Brak autoryzacji");
    }
}
