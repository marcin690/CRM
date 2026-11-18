package wh.plus.crm.service.offerdoc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** HTML → PDF przez Gotenberg (Chromium). Assety z classpath:offer-pdf-assets/ idą obok index.html. */
@Component
public class GotenbergClient {

    @Value("${gotenberg.url:http://localhost:13000}")
    private String url;

    private final RestTemplate rest;

    public GotenbergClient() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(120_000);
        this.rest = new RestTemplate(f);
    }

    /** Assety szablonu wczytane raz przy starcie (fonty, logo, zdjęcia ~600 KB). */
    private final Map<String, byte[]> assets = loadAssets();

    private static Map<String, byte[]> loadAssets() {
        Map<String, byte[]> m = new LinkedHashMap<>();
        try {
            for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath:offer-pdf-assets/*")) {
                if (r.isReadable() && r.getFilename() != null) m.put(r.getFilename(), r.getContentAsByteArray());
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Nie udało się wczytać assetów szablonu PDF: " + e.getMessage(), e);
        }
        return Map.copyOf(m);
    }

    public byte[] htmlToPdf(String html) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("files", file("index.html", html.getBytes(StandardCharsets.UTF_8)));
        assets.forEach((name, bytes) -> form.add("files", file(name, bytes)));
        form.add("preferCssPageSize", "true");
        form.add("printBackground", "true");
        form.add("marginTop", "0");
        form.add("marginBottom", "0");
        form.add("marginLeft", "0");
        form.add("marginRight", "0");
        form.add("skipNetworkIdleEvent", "false"); // czekaj na zdjęcia z WordPressa/MinIO

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        try {
            byte[] pdf = rest.postForObject(url.replaceAll("/+$", "") + "/forms/chromium/convert/html", new HttpEntity<>(form, h), byte[].class);
            if (pdf == null || pdf.length == 0) throw new IllegalStateException("Gotenberg zwrócił pusty PDF.");
            return pdf;
        } catch (org.springframework.web.client.RestClientException e) {
            throw new IllegalStateException("Generator PDF niedostępny: " + e.getMessage());
        }
    }

    private static ByteArrayResource file(String name, byte[] bytes) {
        return new ByteArrayResource(bytes) {
            @Override public String getFilename() { return name; }
        };
    }
}
