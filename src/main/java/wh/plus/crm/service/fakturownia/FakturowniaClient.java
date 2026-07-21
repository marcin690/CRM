package wh.plus.crm.service.fakturownia;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import wh.plus.crm.model.invoice.FakturowniaAccount;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Klient HTTP do Fakturowni — <b>WYŁĄCZNIE ODCZYT (GET)</b>.
 *
 * <p>System CRM kategorycznie nie może modyfikować faktur w Fakturowni.
 * Ta klasa zawiera tylko metody {@code fetch*()} wywołujące {@link HttpMethod#GET}.
 * Nigdy nie dodawaj tu metod POST / PUT / PATCH / DELETE. Jeśli pojawi się
 * potrzeba modyfikacji faktury, musi to zostać zrobione bezpośrednio w
 * interfejsie Fakturowni przez księgowość, nie przez ten system.</p>
 *
 * <p>API Fakturowni: <a href="https://app.fakturownia.pl/api">https://app.fakturownia.pl/api</a>.
 * Endpoint: {@code https://{subdomain}.fakturownia.pl/invoices.json}.</p>
 */
@Slf4j
@Component
public class FakturowniaClient {

    private final RestTemplate restTemplate;
    private final String baseUrlTemplate;

    public FakturowniaClient(
            @Qualifier("fakturowniaRestTemplate") RestTemplate restTemplate,
            @Value("${fakturownia.api.base-url-template:https://%s.fakturownia.pl}") String baseUrlTemplate
    ) {
        // UWAGA: używamy DEDYKOWANEGO bean'a z FakturowniaReadOnlyInterceptor —
        // każdy non-GET request rzuca IllegalStateException zanim leci do sieci.
        // To architekturalna gwarancja read-only, niezależna od kodu tej klasy.
        this.restTemplate = restTemplate;
        this.baseUrlTemplate = baseUrlTemplate;
    }

    /**
     * Wymusza Accept: application/json — bez tego niektóre serwery odpowiadają
     * HTML/redirect na ścieżkę logowania nawet dla URI z .json.
     */
    private HttpEntity<Void> jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(h);
    }

    /**
     * Pobiera (GET) listę faktur dla danego konta i firmy w Fakturowni.
     *
     * @param account konto z apiSubdomain + apiToken
     * @param companyId id firmy w Fakturowni (typowo 1 lub 2 dla kont obsługujących 2 spółki)
     * @param period filtr okresu wg API Fakturowni, np. "this_year", "this_month", "all"
     * @param page numer strony (paginacja Fakturowni, od 1)
     */
    public List<JsonNode> fetchInvoices(FakturowniaAccount account, Long companyId, String period, int page, Boolean income) {
        // income: true = faktury przychodowe, false = koszty/wydatki, null = wszystkie.
        // Fakturownia trzyma przychody i koszty w tym samym /invoices.json, ale
        // DOMYŚLNIE zwraca tylko przychody — koszty trzeba pobrać jawnie income=false.
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl(account))
                .path("/invoices.json")
                .queryParam("api_token", account.getApiToken())
                .queryParam("period", period == null ? "this_year" : period)
                .queryParam("page", page)
                .queryParamIfPresent("company_id", java.util.Optional.ofNullable(companyId))
                .queryParamIfPresent("income", java.util.Optional.ofNullable(income))
                .build(true)
                .toUri();

        log.debug("Fakturownia GET invoices: account={} company={} page={}",
                account.getId(), companyId, page);

        ResponseEntity<JsonNode> response = restTemplate.exchange(uri, HttpMethod.GET, jsonHeaders(), JsonNode.class);
        JsonNode body = response.getBody();
        if (body == null || !body.isArray()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<>(body.size());
        body.forEach(result::add);
        return result;
    }

    /**
     * Pobiera (GET) pojedynczą fakturę po jej zewnętrznym id z Fakturowni.
     */
    public JsonNode fetchInvoiceById(FakturowniaAccount account, Long fakturowniaInvoiceId) {
        // id to Long (brak znaków wymagających kodowania) — wstawiamy wprost,
        // unikając szablonu {id}, który przy build(true) jest odrzucany.
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl(account))
                .path("/invoices/" + fakturowniaInvoiceId + ".json")
                .queryParam("api_token", account.getApiToken())
                .build(true)
                .toUri();

        log.debug("Fakturownia GET invoice id={} account={}", fakturowniaInvoiceId, account.getId());

        ResponseEntity<JsonNode> response = restTemplate.exchange(uri, HttpMethod.GET, jsonHeaders(), JsonNode.class);
        return response.getBody();
    }

    /**
     * Pobiera (GET) dane konta — w tym listę firm/działów (departments).
     * Wynik to obiekt z polami konta i tablicą {@code departments}.
     */
    public JsonNode fetchAccount(FakturowniaAccount account) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl(account))
                .path("/account.json")
                .queryParam("api_token", account.getApiToken())
                .build(true)
                .toUri();

        log.debug("Fakturownia GET account: account={}", account.getId());

        ResponseEntity<JsonNode> response = restTemplate.exchange(uri, HttpMethod.GET, jsonHeaders(), JsonNode.class);
        return response.getBody();
    }

    /**
     * Pobiera (GET) listę projektów z Fakturowni — używana jako słownik
     * do wyboru w {@code ProjectFakturowniaBinding.fakturowniaCategoryName}
     * gdy faktury są kategoryzowane przez "project" zamiast "category".
     */
    public List<JsonNode> fetchProjects(FakturowniaAccount account, Long companyId) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl(account))
                .path("/projects.json")
                .queryParam("api_token", account.getApiToken())
                .queryParamIfPresent("company_id", java.util.Optional.ofNullable(companyId))
                .build(true)
                .toUri();

        log.debug("Fakturownia GET projects: account={} company={}", account.getId(), companyId);

        ResponseEntity<JsonNode> response = restTemplate.exchange(uri, HttpMethod.GET, jsonHeaders(), JsonNode.class);
        JsonNode body = response.getBody();
        if (body == null || !body.isArray()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<>(body.size());
        body.forEach(result::add);
        return result;
    }

    /**
     * Pobiera (GET) listę kategorii Fakturowni dla konta. Przydatne do mapowania
     * pól {@code fakturowniaCategoryName} w {@code ProjectFakturowniaBinding}.
     */
    public List<JsonNode> fetchCategories(FakturowniaAccount account, Long companyId) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl(account))
                .path("/categories.json")
                .queryParam("api_token", account.getApiToken())
                .queryParamIfPresent("company_id", java.util.Optional.ofNullable(companyId))
                .build(true)
                .toUri();

        log.debug("Fakturownia GET categories: account={} company={}", account.getId(), companyId);

        ResponseEntity<JsonNode> response = restTemplate.exchange(uri, HttpMethod.GET, jsonHeaders(), JsonNode.class);
        JsonNode body = response.getBody();
        if (body == null || !body.isArray()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<>(body.size());
        body.forEach(result::add);
        return result;
    }

    private String baseUrl(FakturowniaAccount account) {
        return String.format(baseUrlTemplate, account.getApiSubdomain());
    }
}
