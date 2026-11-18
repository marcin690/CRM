package wh.plus.crm.service.offerdoc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Jedno wywołanie Messages API z wymuszonym schematem JSON (structured outputs). Wzór: SalesInsightsService. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfferAiClient {

    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key:}")
    private String apiKey;
    @Value("${anthropic.offer-model:claude-opus-5-5}")
    private String model;
    @Value("${anthropic.offer-effort:medium}")
    private String effort;
    @Value("${anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;
    @Value("${anthropic.workspace-id:}")
    private String workspaceId;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public JsonNode complete(String system, String user, JsonNode schema) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Brak klucza Anthropic (ANTHROPIC_API_KEY).");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 16000);
            body.put("system", system);
            body.put("fallbacks", "default"); // odmowa klasyfikatora → serwer ponawia na innym modelu
            ObjectNode out = body.putObject("output_config");
            out.put("effort", effort);
            out.putObject("format").put("type", "json_schema").set("schema", schema);
            ObjectNode msg = body.putArray("messages").addObject();
            msg.put("role", "user");
            msg.put("content", user);

            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/v1/messages"))
                    .timeout(Duration.ofMinutes(5)) // duże oferty potrafią się pisać 1-2 min
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("anthropic-beta", "server-side-fallback-2026-07-01")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (workspaceId != null && !workspaceId.isBlank()) req.header("anthropic-workspace-id", workspaceId);

            HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("Anthropic {}: {}", res.statusCode(), res.body().length() > 500 ? res.body().substring(0, 500) : res.body());
                throw new IllegalStateException("AI zwróciło błąd " + res.statusCode() + ". Spróbuj ponownie.");
            }
            JsonNode root = objectMapper.readTree(res.body());
            String stop = root.path("stop_reason").asText();
            if ("refusal".equals(stop)) throw new IllegalStateException("AI odmówiło przygotowania treści. Zmień uwagi i spróbuj ponownie.");
            if ("max_tokens".equals(stop)) throw new IllegalStateException("Odpowiedź AI została ucięta. Spróbuj ponownie.");
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : root.path("content")) {
                if ("text".equals(block.path("type").asText())) sb.append(block.path("text").asText(""));
            }
            if (sb.toString().isBlank()) throw new IllegalStateException("AI zwróciło pustą odpowiedź (" + stop + "). Spróbuj ponownie.");
            return objectMapper.readTree(sb.toString());
        } catch (IllegalStateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Przerwano generowanie treści.");
        } catch (Exception e) {
            log.error("Błąd wywołania AI dla oferty", e);
            throw new IllegalStateException("Błąd połączenia z AI: " + e.getMessage());
        }
    }
}
