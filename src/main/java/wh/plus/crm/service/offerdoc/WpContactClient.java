package wh.plus.crm.service.offerdoc;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import wh.plus.crm.dto.offer.OfferPdfModel.Person;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Dane osób z wyposazenie-hotelowe.pl (post type "contact", pola ACF wystawione w REST):
 * stanowisko, telefon, e-mail, zdjecie_osoby_w_formie_kwadratu. Dopasowanie po imieniu i nazwisku.
 * Błędy WordPressa nie blokują oferty — osoba zostaje z tym, co przyszło z kreatora.
 */
@Component
@Slf4j
public class WpContactClient {

    static final String SITE = "https://wyposazenie-hotelowe.pl";

    private final RestTemplate rest;

    public WpContactClient() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(10_000);
        this.rest = new RestTemplate(f);
    }

    /** Stanowisko, telefon i mail z ACF mają pierwszeństwo; zdjęcie i opis z kreatora zostają, jeśli są. */
    public Person enrich(Person p) {
        if (p == null || p.name() == null || p.name().isBlank()) return p;
        try {
            JsonNode post = findContact(p.name());
            if (post == null) return p;
            JsonNode acf = post.path("acf");
            String photo = firstNonBlank(p.photoUrl(), imageUrl(acf.path("zdjecie_osoby_w_formie_kwadratu")),
                    post.at("/_embedded/wp:featuredmedia/0/source_url").asText(null));
            return new Person(p.name(),
                    firstNonBlank(text(acf, "stanowisko"), p.role()),
                    photo,
                    firstNonBlank(text(acf, "e-mail"), p.email()),
                    firstNonBlank(text(acf, "telefon"), p.phone()),
                    p.helps() == null ? List.of() : p.helps());
        } catch (Exception e) {
            log.warn("WordPress: nie udało się pobrać danych osoby {}: {}", p.name(), e.getMessage());
            return p;
        }
    }

    private JsonNode findContact(String name) {
        // URI (nie String): RestTemplate nie zakoduje go drugi raz, więc „ł” w nazwisku działa
        URI uri = UriComponentsBuilder.fromHttpUrl(SITE + "/wp-json/wp/v2/contact")
                .queryParam("search", name).queryParam("_embed", "true").queryParam("per_page", 5).encode().build().toUri();
        JsonNode list = rest.getForObject(uri, JsonNode.class);
        if (list == null) return null;
        for (JsonNode post : list) {
            if (normalize(post.at("/title/rendered").asText()).equals(normalize(name))) return post;
        }
        return null;
    }

    /** Puste pole ACF przychodzi czasem jako false — bierzemy tylko tekst, inaczej w PDF byłoby „false”. */
    static String text(JsonNode acf, String field) {
        JsonNode n = acf.path(field);
        String v = n.isTextual() ? n.asText().trim() : "";
        return v.isEmpty() ? null : v;
    }

    /** Pole obrazka ACF bywa URL-em albo obiektem z "url"; ID liczbowe pomijamy. */
    private static String imageUrl(JsonNode n) {
        if (n.isTextual() && n.asText().startsWith("http")) return n.asText();
        if (n.isObject() && n.path("url").asText("").startsWith("http")) return n.path("url").asText();
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    static String normalize(String s) {
        return s == null ? "" : s.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
