package wh.plus.crm.service.aivaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import wh.plus.crm.config.DifyConfig.DifyProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cienki klient API Dify (tryb chatflow / advanced-chat).
 * Obsługuje upload pliku ({@code POST /files/upload}) oraz wysłanie wiadomości
 * ({@code POST /chat-messages}, response_mode=blocking).
 */
@Service
@Slf4j
public class DifyClient {

    private final DifyProperties props;
    private final RestTemplate rest;

    public DifyClient(DifyProperties props, @Qualifier("difyRestTemplate") RestTemplate rest) {
        this.props = props;
        this.rest = rest;
    }

    /** Referencja do pliku przekazywana w chat-messages. */
    public record FileRef(String type, String uploadFileId) {}

    /** Wynik wywołania czatu. */
    public record ChatResult(String answer, String conversationId, String messageId, Double costUsd) {}

    private String resolveKey(String agentCode) {
        String key = props.getAgentKeys().get(agentCode);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Brak klucza API Dify dla agenta: " + agentCode
                    + " (ustaw dify.agent-keys." + agentCode + ")");
        }
        return key;
    }

    /**
     * Wysyła plik do Dify i zwraca jego identyfikator (upload_file_id).
     */
    public String uploadFile(String agentCode, byte[] bytes, String filename, String contentType, String user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resolveKey(agentCode));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "plik";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders fileHeaders = new HttpHeaders();
        if (contentType != null) {
            fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        }
        body.add("file", new HttpEntity<>(resource, fileHeaders));
        body.add("user", user);

        ResponseEntity<FileUploadResponse> resp = rest.postForEntity(
                props.getBaseUrl() + "/files/upload",
                new HttpEntity<>(body, headers),
                FileUploadResponse.class);

        FileUploadResponse b = resp.getBody();
        if (b == null || b.id == null) {
            throw new IllegalStateException("Dify nie zwróciło id pliku");
        }
        return b.id;
    }

    /**
     * Wysyła wiadomość do agenta (blocking). Pusty/nullowy conversationId = nowa konwersacja.
     */
    public ChatResult sendMessage(String agentCode, String query, List<FileRef> files,
                                  String conversationId, String user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resolveKey(agentCode));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", new HashMap<>());
        body.put("query", query);
        body.put("response_mode", "blocking");
        body.put("user", user);
        body.put("conversation_id", conversationId == null ? "" : conversationId);

        List<Map<String, Object>> fileList = new ArrayList<>();
        if (files != null) {
            for (FileRef f : files) {
                Map<String, Object> fm = new HashMap<>();
                fm.put("type", f.type());
                fm.put("transfer_method", "local_file");
                fm.put("upload_file_id", f.uploadFileId());
                fileList.add(fm);
            }
        }
        body.put("files", fileList);

        ResponseEntity<ChatResponse> resp = rest.postForEntity(
                props.getBaseUrl() + "/chat-messages",
                new HttpEntity<>(body, headers),
                ChatResponse.class);

        ChatResponse b = resp.getBody();
        if (b == null) {
            throw new IllegalStateException("Pusta odpowiedź z Dify");
        }
        Double cost = null;
        try {
            if (b.metadata != null && b.metadata.usage != null && b.metadata.usage.totalPrice != null) {
                cost = Double.valueOf(b.metadata.usage.totalPrice);
            }
        } catch (NumberFormatException ignore) { /* koszt opcjonalny */ }

        return new ChatResult(b.answer, b.conversationId, b.messageId, cost);
    }

    /**
     * Wysyła ocenę użytkownika do Dify dla danej wiadomości.
     * rating: "like" | "dislike" | null (null cofa ocenę); content: opcjonalny komentarz.
     */
    public void sendFeedback(String agentCode, String messageId, String rating, String content, String user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resolveKey(agentCode));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("rating", (rating == null || rating.isBlank()) ? null : rating);
        body.put("user", user);
        if (content != null && !content.isBlank()) {
            body.put("content", content);
        }

        rest.postForEntity(
                props.getBaseUrl() + "/messages/" + messageId + "/feedbacks",
                new HttpEntity<>(body, headers),
                String.class);
    }

    // --- DTO odpowiedzi Dify (nieznane pola ignorowane) ---

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FileUploadResponse {
        String id;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatResponse {
        String answer;
        @JsonProperty("conversation_id")
        String conversationId;
        @JsonProperty("message_id")
        String messageId;
        Metadata metadata;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Metadata {
        Usage usage;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Usage {
        @JsonProperty("total_price")
        String totalPrice;
        String currency;
    }
}
