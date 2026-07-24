package wh.plus.crm.service.sharepoint;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import wh.plus.crm.dto.sharepoint.SharePointItemDTO;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Klient Microsoft Graph dla SharePoint — TYLKO ODCZYT (token + listowanie plików).
 * Uwierzytelnianie: client credentials (app-only). Wymaga uprawnień aplikacji
 * Sites.Read.All / Files.Read.All z admin consent.
 */
@Slf4j
@Component
public class SharePointGraphClient {

    private final RestTemplate restTemplate;
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String siteUrl;
    private final String graphBase;
    private final String loginBase;

    // Prosty cache tokenu (app-only token jest ważny zwykle ~60 min).
    private volatile String cachedToken;
    private volatile long tokenExpiryEpochMs = 0;
    private volatile String cachedSiteId;

    public SharePointGraphClient(
            RestTemplate restTemplate,
            @Value("${sharepoint.tenant-id:}") String tenantId,
            @Value("${sharepoint.client-id:}") String clientId,
            @Value("${sharepoint.client-secret:}") String clientSecret,
            @Value("${sharepoint.site-url:}") String siteUrl,
            @Value("${sharepoint.graph-base-url:https://graph.microsoft.com/v1.0}") String graphBase,
            @Value("${sharepoint.login-base-url:https://login.microsoftonline.com}") String loginBase) {
        this.restTemplate = restTemplate;
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.siteUrl = siteUrl;
        this.graphBase = graphBase;
        this.loginBase = loginBase;
    }

    public boolean isConfigured() {
        return notBlank(tenantId) && notBlank(clientId) && notBlank(clientSecret) && notBlank(siteUrl);
    }

    private synchronized String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryEpochMs - 60_000) {
            return cachedToken;
        }
        URI uri = URI.create(loginBase + "/" + tenantId + "/oauth2/v2.0/token");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("scope", "https://graph.microsoft.com/.default");

        ResponseEntity<JsonNode> resp = restTemplate.exchange(
                uri, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        JsonNode b = resp.getBody();
        if (b == null || b.get("access_token") == null) {
            throw new IllegalStateException("Nie udało się uzyskać tokenu Graph");
        }
        cachedToken = b.get("access_token").asText();
        long expiresIn = b.has("expires_in") ? b.get("expires_in").asLong() : 3600;
        tokenExpiryEpochMs = System.currentTimeMillis() + expiresIn * 1000;
        return cachedToken;
    }

    private String getSiteId() {
        if (cachedSiteId != null) return cachedSiteId;
        // site-url np. https://contoso.sharepoint.com/sites/Projekty
        URI parsed = URI.create(siteUrl);
        String host = parsed.getHost();
        String path = parsed.getPath() == null ? "" : parsed.getPath();
        // Graph: GET /sites/{host}:/{server-relative-path}
        String url = graphBase + "/sites/" + host + ":" + path;
        JsonNode site = graphGet(URI.create(url));
        if (site == null || site.get("id") == null) {
            throw new IllegalStateException("Nie znaleziono witryny SharePoint: " + siteUrl);
        }
        cachedSiteId = site.get("id").asText();
        return cachedSiteId;
    }

    /** Listuje pliki i foldery w danym folderze (path względem root drive witryny). */
    public List<SharePointItemDTO> listChildren(String path) {
        String siteId = getSiteId();
        String childrenUrl;
        if (path == null || path.isBlank() || "/".equals(path)) {
            childrenUrl = graphBase + "/sites/" + siteId + "/drive/root/children";
        } else {
            String clean = path.replaceAll("^/+", "").replaceAll("/+$", "");
            childrenUrl = UriComponentsBuilder
                    .fromHttpUrl(graphBase + "/sites/" + siteId + "/drive/root:/" + clean + ":/children")
                    .build().toUri().toString();
        }
        JsonNode resp = graphGet(URI.create(childrenUrl));
        List<SharePointItemDTO> result = new ArrayList<>();
        if (resp != null && resp.has("value")) {
            for (JsonNode n : resp.get("value")) {
                boolean isFolder = n.has("folder");
                String mime = n.path("file").path("mimeType").asText(null);
                Long size = n.has("size") ? n.get("size").asLong() : null;
                result.add(new SharePointItemDTO(
                        n.path("id").asText(null),
                        n.path("name").asText(null),
                        n.path("webUrl").asText(null),
                        isFolder, mime, size));
            }
        }
        return result;
    }

    private JsonNode graphGet(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<JsonNode> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        return resp.getBody();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
