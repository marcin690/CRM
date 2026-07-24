package wh.plus.crm.service.fakturownia;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.model.invoice.FakturowniaAccount;
import wh.plus.crm.model.invoice.Invoice;
import wh.plus.crm.model.invoice.InvoiceKind;
import wh.plus.crm.model.invoice.ProjectFakturowniaBinding;
import wh.plus.crm.repository.InvoiceRepository;
import wh.plus.crm.repository.ProjectFakturowniaBindingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Synchronizuje lokalny cache faktur (tabela invoice) z Fakturownią.
 * Wyłącznie odczyt — przez {@link FakturowniaClient} dostępne są tylko metody GET.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FakturowniaSyncService {

    /**
     * Twardy limit stron na pojedyncze wiązanie — zapobiega zapętleniu jeśli
     * Fakturownia zwróci ciągle pełne strony. Konta z dużą liczbą kosztów mają
     * >1400 faktur w roku (≈57 stron × 25/page), więc 200 stron daje zapas.
     */
    private static final int MAX_PAGES = 200;

    /**
     * Statusy z Fakturowni, które oznaczają fakturę opłaconą.
     * Reszta (issued, sent, partial, none) → paid=false.
     */
    private static final Set<String> PAID_STATUSES = Set.of("paid", "paid_in_advance");

    private final FakturowniaClient fakturowniaClient;
    private final ProjectFakturowniaBindingRepository bindingRepository;
    private final InvoiceRepository invoiceRepository;
    private final wh.plus.crm.repository.FakturowniaAccountRepository accountRepository;

    // "this_year" jest bezpiecznym, prawidłowym domyślnym okresem Fakturowni.
    // (Wcześniejsze ":more" było nieprawidłowe bez date_from/date_to.)
    @Value("${fakturownia.sync.period:this_year}")
    private String syncPeriod;

    /** Wynik synchronizacji — zwracany do API, żeby od razu było widać gdzie wpada 0. */
    public record SyncResult(int bindings, int fetched, int matched, int upserted, String period) {}

    private record BindingSyncResult(int fetched, int matched) {}

    /**
     * Pełna synchronizacja wszystkich aktywnych kont × bindingów.
     * Konfigurowalna przez {@code fakturownia.sync.cron} (domyślnie co 15 min).
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "${fakturownia.sync.cron:0 */15 * * * ?}")
    public void scheduledSync() {
        log.info("Fakturownia sync — start");
        SyncResult result = syncAll();
        log.info("Fakturownia sync — done: {}", result);
    }

    @Transactional
    public SyncResult syncAll() {
        int bindings = 0, fetched = 0, matched = 0;
        // Jedno zapytanie: wszystkie bindingi z aktywnymi kontami.
        List<ProjectFakturowniaBinding> all = bindingRepository.findAllByAccount_EnabledTrue();
        if (all.isEmpty()) {
            log.warn("Fakturownia syncAll: brak wiązań z aktywnym kontem — nic do pobrania. "
                    + "Dodaj wiązanie konta w zakładce Ustawienia projektu i upewnij się, że konto jest 'enabled'.");
        }
        for (ProjectFakturowniaBinding binding : all) {
            bindings++;
            try {
                BindingSyncResult r = syncBinding(binding);
                fetched += r.fetched();
                matched += r.matched();
            } catch (Exception e) {
                log.warn("Sync failed for binding={} (account={}): {}",
                        binding.getId(),
                        binding.getAccount() != null ? binding.getAccount().getId() : null,
                        e.getMessage());
            }
        }
        SyncResult result = new SyncResult(bindings, fetched, matched, matched, syncPeriod);
        log.info("Fakturownia syncAll: {}", result);
        return result;
    }

    public BindingSyncResult syncBinding(ProjectFakturowniaBinding binding) {
        FakturowniaAccount account = binding.getAccount();
        // Mapa id→nazwa kategorii konta — pozwala dopasować fakturę po category_id,
        // bo Fakturownia w fakturze zwraca zwykle category_id, a nie nazwę tekstem.
        Map<Long, String> categoryNames = loadCategoryNames(account, binding.getCompanyId());

        // Rola wiązania decyduje, które faktury pobrać: REVENUE = przychody (income=true),
        // COST = koszty/wydatki (income=false). Bez tego Fakturownia zwraca tylko przychody.
        Boolean income = binding.getRole() == wh.plus.crm.model.invoice.FakturowniaRole.COST
                ? Boolean.FALSE : Boolean.TRUE;

        int fetched = 0, matched = 0;
        // Diagnostyka: co realnie mają faktury w polu kategorii.
        Map<String, Integer> seenCategories = new HashMap<>();
        String firstInvoiceKeys = null;
        for (int page = 1; page <= MAX_PAGES; page++) {
            List<JsonNode> invoices = fakturowniaClient.fetchInvoices(
                    account, binding.getCompanyId(), syncPeriod, page, income);
            // Polegamy WYŁĄCZNIE na pustej odpowiedzi jako warunek końca.
            if (invoices.isEmpty()) break;
            fetched += invoices.size();
            for (JsonNode node : invoices) {
                if (firstInvoiceKeys == null) firstInvoiceKeys = fieldNames(node);
                seenCategories.merge(categoryRepr(node, categoryNames), 1, Integer::sum);
                if (!matchesBindingCategory(node, binding, categoryNames)) continue;
                upsertInvoice(node, account, binding);
                matched++;
            }
        }
        if (fetched == 0) {
            log.warn("Sync binding={} (account={}, company={}, period={}): pobrano 0 faktur z Fakturowni. "
                    + "Sprawdź token/subdomenę konta, company_id oraz okres (period).",
                    binding.getId(), account.getId(), binding.getCompanyId(), syncPeriod);
        } else if (matched == 0) {
            log.warn("Sync binding={} (account={}): pobrano {} faktur, ale 0 pasuje do kategorii '{}'. "
                    + "Sprawdź czy nazwa kategorii dokładnie odpowiada kategorii faktur w Fakturowni.",
                    binding.getId(), account.getId(), fetched, binding.getFakturowniaCategoryName());
            log.warn("DIAGNOSTYKA binding={}: słownik kategorii konta ({}): {}",
                    binding.getId(), categoryNames.size(), categoryNames);
            log.warn("DIAGNOSTYKA binding={}: kategorie na pobranych fakturach (repr→liczba): {}",
                    binding.getId(), seenCategories);
            log.warn("DIAGNOSTYKA binding={}: pola pierwszej faktury z Fakturowni: {}",
                    binding.getId(), firstInvoiceKeys);
        } else {
            log.info("Sync binding={} (account={}): pobrano {} faktur, dopasowano {} (kategoria '{}').",
                    binding.getId(), account.getId(), fetched, matched, binding.getFakturowniaCategoryName());
        }
        return new BindingSyncResult(fetched, matched);
    }

    /**
     * Diagnostyka: zwraca słownik kategorii konta + rozkład kategorii na pierwszej
     * stronie faktur + surowy JSON przykładowej faktury. Pozwala zobaczyć, w jakim
     * polu i pod jaką wartością Fakturownia trzyma kategorię — żeby poprawnie
     * skonfigurować dopasowanie. Wyłącznie odczyt.
     */
    public Map<String, Object> debugFirstPage(Long accountId, Long companyId) {
        FakturowniaAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        Map<Long, String> categoryNames = loadCategoryNames(account, companyId);
        List<JsonNode> invoices = fakturowniaClient.fetchInvoices(account, companyId, syncPeriod, 1, null);

        Map<String, Integer> distribution = new java.util.LinkedHashMap<>();
        for (JsonNode n : invoices) distribution.merge(categoryRepr(n, categoryNames), 1, Integer::sum);

        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("period", syncPeriod);
        out.put("companyId", companyId);
        out.put("invoicesOnFirstPage", invoices.size());
        out.put("categoriesDict", categoryNames);
        out.put("categoryDistribution", distribution);
        out.put("sampleInvoiceFields", invoices.isEmpty() ? null : fieldNames(invoices.get(0)));
        out.put("sampleInvoice", invoices.isEmpty() ? null : invoices.get(0));
        return out;
    }

    /** Czytelna reprezentacja kategorii faktury — do logu diagnostycznego. */
    private String categoryRepr(JsonNode node, Map<Long, String> categoryNames) {
        String text = firstNonBlank(optText(node, "category"), optText(node, "category_name"));
        if (text != null) return "text='" + text + "'";
        Long catId = optLong(node, "category_id");
        if (catId == null) return "brak (category_id=null)";
        String resolved = categoryNames.get(catId);
        return "id=" + catId + (resolved != null ? " ('" + resolved + "')" : " (poza słownikiem)");
    }

    /** Lista nazw pól pierwszej faktury — pozwala zobaczyć, jak Fakturownia nazywa pole kategorii. */
    private String fieldNames(JsonNode node) {
        java.util.List<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return String.join(", ", names);
    }

    private Map<Long, String> loadCategoryNames(FakturowniaAccount account, Long companyId) {
        try {
            Map<Long, String> map = new HashMap<>();
            for (JsonNode c : fakturowniaClient.fetchCategories(account, companyId)) {
                Long id = optLong(c, "id");
                String name = optText(c, "name");
                if (id != null && name != null) map.put(id, name);
            }
            return map;
        } catch (Exception e) {
            log.warn("Nie udało się pobrać słownika kategorii (account={}, company={}): {}",
                    account.getId(), companyId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Dopasowanie faktury do kategorii wiązania. Odporne na to, jak Fakturownia
     * zwraca kategorię: pole tekstowe {@code category}/{@code category_name}, albo
     * numeryczne {@code category_id} (rozwiązywane przez słownik kategorii konta).
     */
    private boolean matchesBindingCategory(JsonNode node, ProjectFakturowniaBinding binding,
                                           Map<Long, String> categoryNames) {
        String filter = binding.getFakturowniaCategoryName();
        if (filter == null || filter.isBlank()) return true;

        String catText = firstNonBlank(optText(node, "category"), optText(node, "category_name"));
        if (catText == null) {
            Long catId = optLong(node, "category_id");
            if (catId != null) catText = categoryNames.get(catId);
        }
        if (catText == null || catText.isBlank()) return false;
        return filter.trim().equalsIgnoreCase(catText.trim());
    }

    private void upsertInvoice(JsonNode node, FakturowniaAccount account, ProjectFakturowniaBinding binding) {
        Long externalId = optLong(node, "id");
        if (externalId == null) return;

        Invoice invoice = invoiceRepository
                .findByFakturowniaIdAndAccount(externalId, account)
                .orElseGet(Invoice::new);

        String status = optText(node, "status");
        LocalDate paidDate = optDate(node, "paid_date");

        invoice.setFakturowniaId(externalId);
        invoice.setAccount(account);
        invoice.setBinding(binding);
        invoice.setCompanyId(binding.getCompanyId());
        invoice.setNumber(optText(node, "number"));
        invoice.setTitle(optText(node, "title"));
        invoice.setIssueDate(optDate(node, "issue_date"));
        invoice.setSellDate(optDate(node, "sell_date"));
        invoice.setPaymentDate(optDate(node, "payment_to"));
        invoice.setNetValue(optDecimal(node, "price_net"));
        invoice.setGrossValue(optDecimal(node, "price_gross"));
        invoice.setVatValue(optDecimal(node, "price_tax"));
        invoice.setCurrency(optText(node, "currency"));
        invoice.setKind(parseKind(optText(node, "kind")));
        invoice.setStatus(status);
        // paid = status w whiteliście LUB Fakturownia podała paid_date.
        invoice.setPaid(
                (status != null && PAID_STATUSES.contains(status.toLowerCase()))
                || paidDate != null);
        invoice.setBuyerName(firstNonBlank(
                optText(node, "buyer_name"),
                optTextDeep(node, "buyer", "name")));
        invoice.setSellerName(firstNonBlank(
                optText(node, "seller_name"),
                optTextDeep(node, "seller", "name")));
        invoice.setFakturowniaUrl(firstNonBlank(
                optText(node, "view_url"),
                optText(node, "pretty_url")));
        invoice.setLastSyncedAt(LocalDateTime.now());

        invoiceRepository.save(invoice);
    }

    private static final Map<String, InvoiceKind> KIND_MAP = Map.ofEntries(
            Map.entry("vat", InvoiceKind.VAT),
            Map.entry("proforma", InvoiceKind.PROFORMA),
            Map.entry("correction", InvoiceKind.CORRECTION),
            Map.entry("vat_correction", InvoiceKind.CORRECTION),
            Map.entry("advance", InvoiceKind.ADVANCE),
            Map.entry("final", InvoiceKind.FINAL),
            Map.entry("kp", InvoiceKind.RECEIPT),
            Map.entry("kw", InvoiceKind.RECEIPT),
            Map.entry("receipt", InvoiceKind.RECEIPT),
            Map.entry("bill", InvoiceKind.OTHER)
    );

    private InvoiceKind parseKind(String kind) {
        if (kind == null) return InvoiceKind.OTHER;
        return KIND_MAP.getOrDefault(kind.toLowerCase(), InvoiceKind.OTHER);
    }

    private Long optLong(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v != null && !v.isNull() ? v.asLong() : null;
    }

    private String optText(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    private String optTextDeep(JsonNode n, String parent, String child) {
        JsonNode p = n.get(parent);
        if (p == null || p.isNull()) return null;
        return optText(p, child);
    }

    private LocalDate optDate(JsonNode n, String f) {
        String s = optText(n, f);
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private BigDecimal optDecimal(JsonNode n, String f) {
        String s = optText(n, f);
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
