package wh.plus.crm.service.fakturownia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import wh.plus.crm.model.invoice.FakturowniaAccount;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Test obronny: weryfikuje, że {@link FakturowniaClient} wykonuje
 * <b>wyłącznie żądania GET</b> do API Fakturowni. Jeśli kiedyś ktoś
 * doda metodę POST/PUT/DELETE, ten test wymusi zauważenie zmiany.
 *
 * Wymóg biznesowy: system CRM nigdy nie modyfikuje faktur — tylko odczytuje.
 */
class FakturowniaClientReadOnlyTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private FakturowniaClient client;
    private FakturowniaAccount account;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new FakturowniaClient(restTemplate, "https://%s.fakturownia.pl");

        account = new FakturowniaAccount();
        account.setId(1L);
        account.setApiSubdomain("whplus");
        account.setApiToken("test-token");
    }

    @Test
    void fetchInvoices_uses_GET_only() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://whplus.fakturownia.pl/invoices.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.fetchInvoices(account, 1L, "this_year", 1, null);

        mockServer.verify();
    }

    @Test
    void fetchInvoiceById_uses_GET_only() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://whplus.fakturownia.pl/invoices/42.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":42}", MediaType.APPLICATION_JSON));

        client.fetchInvoiceById(account, 42L);

        mockServer.verify();
    }

    @Test
    void fetchCategories_uses_GET_only() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://whplus.fakturownia.pl/categories.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.fetchCategories(account, 1L);

        mockServer.verify();
    }

    /**
     * Test introspekcyjny: na poziomie typu sprawdza, że klasa
     * {@link FakturowniaClient} nie udostępnia żadnej publicznej metody,
     * której nazwa sugeruje modyfikację (post/put/patch/delete/create/update/save).
     */
    @Test
    void no_mutating_methods_exposed() {
        java.util.Arrays.stream(FakturowniaClient.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .forEach(m -> {
                    String lower = m.getName().toLowerCase();
                    if (lower.startsWith("post")
                            || lower.startsWith("put")
                            || lower.startsWith("patch")
                            || lower.startsWith("delete")
                            || lower.startsWith("create")
                            || lower.startsWith("update")
                            || lower.startsWith("save")) {
                        throw new AssertionError(
                                "FakturowniaClient nie może mieć metod modyfikujących. Znaleziono: " + m.getName());
                    }
                });
    }

    /**
     * Architekturalny test interceptora — gwarancja niezależna od kodu klienta.
     * RestTemplate z {@link FakturowniaReadOnlyInterceptor} MUSI odrzucić każdy
     * non-GET request zanim trafi do sieci. Nawet jeśli ktoś w przyszłości
     * wywoła {@code restTemplate.postForObject(...)} bezpośrednio — dostanie
     * {@link IllegalStateException}.
     */
    @org.junit.jupiter.api.Nested
    class InterceptorGuarantees {

        private RestTemplate guardedTemplate;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            guardedTemplate = new RestTemplate();
            guardedTemplate.getInterceptors().add(new FakturowniaReadOnlyInterceptor());
        }

        @Test
        void blocks_POST_to_fakturownia() {
            org.junit.jupiter.api.Assertions.assertThrows(
                    Exception.class,
                    () -> guardedTemplate.postForObject(
                            "https://whplus.fakturownia.pl/invoices.json",
                            "{}",
                            String.class),
                    "POST do Fakturowni musi być zablokowany przez interceptor"
            );
        }

        @Test
        void blocks_PUT_to_fakturownia() {
            org.junit.jupiter.api.Assertions.assertThrows(
                    Exception.class,
                    () -> guardedTemplate.put(
                            "https://whplus.fakturownia.pl/invoices/1.json",
                            "{}"),
                    "PUT do Fakturowni musi być zablokowany przez interceptor"
            );
        }

        @Test
        void blocks_DELETE_to_fakturownia() {
            org.junit.jupiter.api.Assertions.assertThrows(
                    Exception.class,
                    () -> guardedTemplate.delete("https://whplus.fakturownia.pl/invoices/1.json"),
                    "DELETE do Fakturowni musi być zablokowany przez interceptor"
            );
        }

        @Test
        void blocks_non_Fakturownia_host() {
            // Defense in depth: ten template MUSI być używany TYLKO do Fakturowni.
            // Pomyłkowe wpisanie innego hosta powinno zostać zablokowane.
            org.junit.jupiter.api.Assertions.assertThrows(
                    Exception.class,
                    () -> guardedTemplate.getForObject(
                            "https://api.nbp.pl/api/exchangerates/rates/A/EUR",
                            String.class),
                    "RestTemplate Fakturowni nie może być używany do innych hostów"
            );
        }
    }
}
