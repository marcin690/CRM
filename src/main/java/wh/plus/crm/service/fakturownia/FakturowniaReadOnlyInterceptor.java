package wh.plus.crm.service.fakturownia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Architekturalna gwarancja read-only dla integracji z Fakturownią.
 *
 * <p>Każdy request przechodzący przez {@code fakturowniaRestTemplate} musi:</p>
 * <ol>
 *   <li>Być metodą {@link HttpMethod#GET} (lub HEAD/OPTIONS) — wszystko inne rzuca
 *       {@link IllegalStateException} ZANIM request poleci do sieci.</li>
 *   <li>Być skierowany do hosta {@code *.fakturownia.pl} — niezgodne hosty też są
 *       odrzucane (defense in depth — chroni przed pomyłkowym wpisaniem URL).</li>
 * </ol>
 *
 * <p><b>Wymóg biznesowy:</b> system CRM nigdy nie modyfikuje faktur w Fakturowni.
 * Ten interceptor jest fizyczną realizacją tego wymogu — niezależnie od tego,
 * co napisze przyszły kod. Test {@code FakturowniaClientReadOnlyTest}
 * weryfikuje to zachowanie.</p>
 */
@Slf4j
public class FakturowniaReadOnlyInterceptor implements ClientHttpRequestInterceptor {

    private static final java.util.Set<HttpMethod> ALLOWED_METHODS = java.util.Set.of(
            HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS
    );

    /**
     * Segmenty ścieżek odpowiadające akcjom Fakturowni ze skutkiem ubocznym.
     * Niektóre z nich bywają wołane przez GET, więc sama blokada metod nie wystarcza.
     * Każdy URI zawierający którykolwiek z tych segmentów jest odrzucany —
     * to gwarantuje, że integracja NIGDY nie zmieni stanu faktury.
     */
    private static final java.util.List<String> FORBIDDEN_PATH_SEGMENTS = java.util.List.of(
            "change_status", "send_by_email", "deliver", "mark_as", "cancel",
            "/create", "/update", "/destroy"
    );

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        HttpMethod method = request.getMethod();
        if (!ALLOWED_METHODS.contains(method)) {
            String msg = "Blocked: Fakturownia integration is READ-ONLY. " +
                    "Method " + method + " is forbidden. " +
                    "URI: " + request.getURI();
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        String host = request.getURI().getHost();
        if (host == null || !host.endsWith("fakturownia.pl")) {
            String msg = "Blocked: fakturowniaRestTemplate used for non-Fakturownia host: " + host +
                    " (URI: " + request.getURI() + "). " +
                    "Use a different RestTemplate for other services.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // Defense in depth: nawet GET nie może trafić w akcję zmieniającą stan.
        String path = request.getURI().getPath();
        if (path != null) {
            String lower = path.toLowerCase();
            for (String segment : FORBIDDEN_PATH_SEGMENTS) {
                if (lower.contains(segment)) {
                    String msg = "Blocked: Fakturownia integration is READ-ONLY. " +
                            "Path contains a state-changing action '" + segment + "'. " +
                            "URI: " + request.getURI();
                    log.error(msg);
                    throw new IllegalStateException(msg);
                }
            }
        }

        return execution.execute(request, body);
    }
}
