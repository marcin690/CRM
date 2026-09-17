package wh.plus.crm.service.aichat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wh.plus.crm.config.DifyConfig.DifyProperties;
import wh.plus.crm.service.aivaluation.DifyClient.FileRef;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Klient streamingowy Dify (SSE) dla czatów AI. Używa {@link HttpClient} zamiast RestTemplate,
 * bo czyta strumień {@code text/event-stream} linia po linii i przekazuje fragmenty dalej.
 *
 * <p>Klucz API rozwiązywany po {@code agentCode} ({@code dify.agent-keys.<code>}), pole {@code user}
 * zawsze podane przez wołającego (backend liczy je z sesji). Ta klasa nie zna encji ani sesji.
 */
@Service
@Slf4j
public class DifyChatStreamer {

    private final DifyProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public DifyChatStreamer(DifyProperties props) {
        this.props = props;
    }

    /** Dane zużycia z {@code message_end.metadata.usage}. */
    public record UsageInfo(int promptTokens, int completionTokens, String totalPrice, double latencySeconds) {}

    /** Callback zdarzeń streamu. Wszystkie metody wołane z wątku pompującego. */
    public interface Handler {
        /** Pierwsze zdarzenie z conversation_id (nowa rozmowa dostaje id od Dify). */
        void onStart(String difyConversationId, String taskId);
        void onDelta(String text);
        void onEnd(String messageId, UsageInfo usage);
        /** Przyjazny komunikat (bez szczegółów Dify). */
        void onError(String friendlyMessage);
    }

    private String resolveKey(String agentCode) {
        String key = props.getAgentKeys().get(agentCode);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Brak klucza API Dify dla: " + agentCode);
        }
        return key;
    }

    /**
     * Wysyła wiadomość w trybie streaming i pompuje zdarzenia do {@code handler}.
     * {@code cancelled} pozwala przerwać czytanie, gdy przeglądarka się rozłączy.
     */
    public void stream(String agentCode, String query, List<FileRef> files, String difyConversationId,
                       String user, Handler handler, BooleanSupplier cancelled) {
        ObjectNode body = mapper.createObjectNode();
        body.set("inputs", mapper.createObjectNode());
        body.put("query", query);
        body.put("response_mode", "streaming");
        body.put("user", user);
        body.put("conversation_id", difyConversationId == null ? "" : difyConversationId);
        body.put("auto_generate_name", true);
        ArrayNode fileArr = body.putArray("files");
        if (files != null) {
            for (FileRef f : files) {
                ObjectNode fn = fileArr.addObject();
                fn.put("type", f.type());
                fn.put("transfer_method", "local_file");
                fn.put("upload_file_id", f.uploadFileId());
            }
        }

        HttpRequest req;
        try {
            req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/chat-messages"))
                    .header("Authorization", "Bearer " + resolveKey(agentCode))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds(60)) // cap na czas do pierwszej odpowiedzi (nagłówki) — dalej rządzi watchdog
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            log.error("AI chat: budowa requestu nie powiodła się: {}", e.getMessage());
            handler.onError("Nie udało się wysłać wiadomości");
            return;
        }

        InputStream bodyStream = null;
        Thread watchdog = null;
        final AtomicBoolean timedOut = new AtomicBoolean(false);
        try {
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() / 100 != 2) {
                String raw = readAll(resp.body());
                log.warn("AI chat: Dify HTTP {} body={}", resp.statusCode(), raw);
                handler.onError(mapError(raw));
                return;
            }

            boolean started = false;
            String lastConv = difyConversationId;
            String lastTask = null;

            // Watchdog: zamyka strumień gdy klient się rozłączy (cancelled) lub po twardym deadline,
            // odblokowując readLine() — bez tego wątek puli wisiałby przy zawieszonym Dify.
            bodyStream = resp.body();
            final InputStream streamToClose = bodyStream;
            final long deadline = System.currentTimeMillis() + 5 * 60_000; // twardy cap < timeout emittera (5,5 min)
            watchdog = new Thread(() -> {
                try {
                    while (!cancelled.getAsBoolean() && System.currentTimeMillis() < deadline) {
                        Thread.sleep(300);
                    }
                    if (!cancelled.getAsBoolean()) timedOut.set(true); // wyjście przez deadline, nie rozłączenie
                } catch (InterruptedException ignore) {
                    return; // normalne zakończenie (interrupt z finally) — strumień już zamknięty przez try-with-resources
                }
                try { streamToClose.close(); } catch (Exception ignore) { }
            });
            watchdog.setDaemon(true);
            watchdog.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cancelled.getAsBoolean()) {
                        log.debug("AI chat: przerwane przez rozłączenie klienta");
                        break;
                    }
                    if (line.isEmpty() || !line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;

                    JsonNode ev;
                    try {
                        ev = mapper.readTree(data);
                    } catch (Exception parse) {
                        continue; // ignoruj nie-JSON linie (komentarze SSE itp.)
                    }

                    String event = ev.path("event").asText("");
                    if (ev.hasNonNull("conversation_id")) lastConv = ev.get("conversation_id").asText();
                    if (ev.hasNonNull("task_id")) lastTask = ev.get("task_id").asText();

                    if (!started && lastConv != null && !lastConv.isBlank()) {
                        started = true;
                        handler.onStart(lastConv, lastTask);
                    }

                    switch (event) {
                        case "message", "agent_message" -> {
                            String chunk = ev.path("answer").asText("");
                            if (!chunk.isEmpty()) handler.onDelta(chunk);
                        }
                        case "message_end" -> {
                            String messageId = ev.path("message_id").asText(ev.path("id").asText(""));
                            handler.onEnd(messageId, parseUsage(ev));
                        }
                        case "error" -> {
                            log.warn("AI chat: zdarzenie error z Dify: {}", data);
                            handler.onError("Wystąpił błąd podczas generowania odpowiedzi");
                        }
                        default -> { /* workflow_started, node_*, ping, tts_message itp. — pomijamy */ }
                    }
                }
            }
        } catch (Exception e) {
            if (cancelled.getAsBoolean()) {
                log.debug("AI chat: strumień zamknięty po rozłączeniu klienta");
            } else if (timedOut.get()) {
                log.warn("AI chat: przekroczono limit czasu odpowiedzi");
                handler.onError("Odpowiedź trwała zbyt długo — spróbuj zawęzić pytanie");
            } else {
                log.error("AI chat: błąd streamu: {}", e.getMessage());
                handler.onError("Asystent nie odpowiada, spróbuj za chwilę");
            }
        } finally {
            if (watchdog != null) watchdog.interrupt();
            if (bodyStream != null) { try { bodyStream.close(); } catch (Exception ignore) { } }
        }
    }

    // ===== Operacje pomocnicze (blocking, RestTemplate-free) =====

    /** GET /parameters — opening_statement + suggested_questions do UI. */
    public JsonNode getParameters(String agentCode, String user) throws Exception {
        String url = props.getBaseUrl() + "/parameters?user=" + enc(user);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + resolveKey(agentCode))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) throw new IllegalStateException("parameters HTTP " + resp.statusCode());
        return mapper.readTree(resp.body());
    }

    /** GET /messages — historia (pary query/answer). */
    public JsonNode getMessages(String agentCode, String difyConversationId, String user, String firstId, int limit) throws Exception {
        StringBuilder url = new StringBuilder(props.getBaseUrl())
                .append("/messages?user=").append(enc(user))
                .append("&conversation_id=").append(enc(difyConversationId))
                .append("&limit=").append(limit);
        if (firstId != null && !firstId.isBlank()) url.append("&first_id=").append(enc(firstId));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("Authorization", "Bearer " + resolveKey(agentCode))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) throw new IllegalStateException("messages HTTP " + resp.statusCode());
        return mapper.readTree(resp.body());
    }

    /** POST /conversations/:cid/name — zmiana tytułu. */
    public void renameConversation(String agentCode, String difyConversationId, String name, String user) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("name", name);
        body.put("user", user);
        sendJson("POST", props.getBaseUrl() + "/conversations/" + difyConversationId + "/name", agentCode, body);
    }

    /** DELETE /conversations/:cid — usunięcie po stronie Dify. */
    public void deleteConversation(String agentCode, String difyConversationId, String user) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("user", user);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + "/conversations/" + difyConversationId))
                .header("Authorization", "Bearer " + resolveKey(agentCode))
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        // 404 z Dify jest OK — i tak oznaczymy jako usunięte w CRM.
    }

    /** POST /chat-messages/:task/stop — zatrzymanie generowania. */
    public void stopGeneration(String agentCode, String taskId, String user) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("user", user);
        sendJson("POST", props.getBaseUrl() + "/chat-messages/" + taskId + "/stop", agentCode, body);
    }

    private void sendJson(String method, String url, String agentCode, ObjectNode body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + resolveKey(agentCode))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private UsageInfo parseUsage(JsonNode ev) {
        JsonNode u = ev.path("metadata").path("usage");
        return new UsageInfo(
                u.path("prompt_tokens").asInt(0),
                u.path("completion_tokens").asInt(0),
                u.path("total_price").asText("0"),
                u.path("latency").asDouble(0));
    }

    private String mapError(String raw) {
        String code = "";
        try { code = mapper.readTree(raw).path("code").asText(""); } catch (Exception ignore) {}
        return switch (code) {
            case "provider_not_initialize", "provider_quota_exceeded",
                 "model_currently_not_support", "completion_request_error" ->
                    "Model jest chwilowo niedostępny, spróbuj ponownie za chwilę";
            case "invalid_param" -> "Nie udało się wysłać wiadomości";
            default -> "Asystent nie odpowiada, spróbuj za chwilę";
        };
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String readAll(InputStream in) {
        try {
            List<String> lines = new ArrayList<>();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String l;
            while ((l = r.readLine()) != null) lines.add(l);
            return String.join("\n", lines);
        } catch (Exception e) {
            return "";
        }
    }
}
