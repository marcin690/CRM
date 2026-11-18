package wh.plus.crm.service.offerdoc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import wh.plus.crm.dto.offer.OfferPdfModel;
import wh.plus.crm.dto.offer.OfferPdfModel.*;
import wh.plus.crm.model.comment.Comment;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.offer.OfferItem;
import wh.plus.crm.model.offer.document.OfferDocument;
import wh.plus.crm.model.offer.document.OfferFaq;
import wh.plus.crm.repository.CommentRepository;
import wh.plus.crm.repository.OfferDocumentRepository;
import wh.plus.crm.repository.OfferFaqRepository;
import wh.plus.crm.repository.OfferRepository;
import wh.plus.crm.service.MinioUploadService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferDocumentService {

    /** Znacznik braku faktu od AI: [uzupełnij: …], też z wielkiej litery i w wariancie „[do uzupełnienia]”. */
    static final Pattern MISSING = Pattern.compile("\\[\\s*(do\\s+)?uzupe[łl]n", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    static final String OTHER = "Inne";
    public static final String DRAFTING = "DRAFTING", RENDERING = "RENDERING", READY = "READY", FAILED = "FAILED";

    private final OfferRepository offerRepository;
    private final OfferDocumentRepository documentRepository;
    private final OfferFaqRepository faqRepository;
    private final CommentRepository commentRepository;
    private final OfferAiClient aiClient;
    private final GotenbergClient gotenberg;
    private final WpContactClient wp;
    private final MinioUploadService minio;
    private final ITemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;

    // AI (1-2 min) i Gotenberg działają w tle, poza transakcją i poza open-in-view, żeby nie trzymać połączeń z bazą.
    // shortcut: pula w pamięci — zadania przerwane restartem kończą jako FAILED przy odczycie (stale()); przy wielu instancjach backendu przejść na kolejkę.
    private final ExecutorService background = Executors.newFixedThreadPool(4);

    @PreDestroy
    void shutdown() { background.shutdown(); }

    /** Co handlowiec wybrał w kreatorze. similar/team przychodzą z WordPressa (frontend), telefony dociąga backend. */
    public record DraftRequest(String clientType, String projectName, List<String> scope, List<String> priorities,
                               List<Project> similar, List<Person> team, List<Long> faqIds, String notes,
                               List<Attachment> attachments) {}

    /** Poprawki z podglądu: treść + przypisanie pozycji do grup (kwoty i tak liczy serwer). */
    public record UpdateRequest(OfferPdfModel content, Map<Long, String> itemGroups) {}

    private record DraftInput(String prompt, List<String> groups, String clientType, String projectName,
                              String clientName, Long offerId, Person seller) {}

    @Transactional
    public List<OfferDocument> list(Long offerId) {
        List<OfferDocument> docs = documentRepository.findByOffer_IdOrderByIdDesc(offerId);
        docs.forEach(this::stale);
        return docs;
    }

    @Transactional
    public OfferDocument get(Long id) {
        OfferDocument doc = documentRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Nie ma dokumentu oferty " + id));
        stale(doc);
        return doc;
    }

    /** Zadanie „w toku” starsze niż 15 min nie żyje (np. restart backendu) — pokaż błąd zamiast wiecznego spinnera. */
    private void stale(OfferDocument d) {
        boolean busy = DRAFTING.equals(d.getStatus()) || RENDERING.equals(d.getStatus());
        LocalDateTime since = d.getLastModifiedDate() != null ? d.getLastModifiedDate() : d.getCreationDate();
        if (busy && since != null && since.isBefore(LocalDateTime.now().minusMinutes(15))) {
            d.setError("Przerwano przetwarzanie. Spróbuj ponownie.");
            d.setStatus(DRAFTING.equals(d.getStatus()) ? FAILED : READY);
        }
    }

    /** Zakłada dokument w statusie DRAFTING i zleca AI w tle. Frontend odpytuje GET /offer-documents/{id}. */
    @Transactional
    public OfferDocument draft(Long offerId, DraftRequest req) {
        Offer offer = offerRepository.findById(offerId).orElseThrow(() -> new NoSuchElementException("Nie ma oferty " + offerId));
        if (offer.getOfferItemList() == null || offer.getOfferItemList().isEmpty()) {
            throw new IllegalStateException("Oferta nie ma pozycji. Dodaj pozycje do oferty, żeby przygotować wycenę.");
        }
        OfferDocument doc = new OfferDocument();
        doc.setOffer(offer);
        doc.setClientType("RETURNING".equals(req.clientType()) ? "RETURNING" : "NEW");
        doc.setStatus(DRAFTING);
        OfferDocument saved = documentRepository.save(doc);
        Long docId = saved.getId();
        afterCommit(() -> background.submit(() -> runDraft(docId, offerId, req)));
        return saved;
    }

    private void runDraft(Long docId, Long offerId, DraftRequest req) {
        try {
            DraftInput in = tx.execute(s -> draftInput(offerId, req));
            JsonNode ai = aiClient.complete(SYSTEM, in.prompt(), schema(in.groups()));
            requireComplete(ai);
            List<Person> team = (req.team() == null ? List.<Person>of() : req.team()).stream().map(wp::enrich).toList();
            Person seller = wp.enrich(in.seller());
            Map<Long, String> itemGroups = new HashMap<>();
            for (JsonNode g : ai.path("groups")) itemGroups.put(g.path("itemId").asLong(), g.path("group").asText(OTHER));

            tx.executeWithoutResult(s -> {
                OfferDocument doc = documentRepository.findById(docId).orElseThrow();
                if (!DRAFTING.equals(doc.getStatus())) return; // uznane za przerwane — handlowiec mógł już zacząć od nowa
                Offer offer = doc.getOffer();
                OfferPdfModel model = new OfferPdfModel(
                        in.clientType(), in.projectName(), in.clientName(), in.offerId(),
                        today(),
                        seller,
                        scopeOf(req),
                        req.priorities() == null ? List.of() : req.priorities(),
                        ai.path("headline").asText(),
                        strings(ai.path("situation")),
                        ai.path("goal").asText(),
                        convert(ai.path("schedule"), new TypeReference<List<Step>>() {}),
                        strings(ai.path("needs")),
                        convert(ai.path("optimization"), new TypeReference<List<Item>>() {}),
                        buildPricing(offer.getOfferItemList(), itemGroups, in.groups(), currency(offer)),
                        req.similar() == null ? List.of() : req.similar(),
                        team,
                        chosenFaqs(req.faqIds()),
                        "NEW".equals(in.clientType()) ? REFERENCE_LETTERS : List.of(),
                        req.attachments() == null ? List.of() : req.attachments());
                doc.setContent(write(prepare(model, doc, itemGroups, model.date())));
                doc.setItemGroups(write(itemGroups));
                doc.setStatus(READY);
                doc.setError(null);
            });
        } catch (Exception e) {
            log.warn("Nie udało się przygotować treści dokumentu {}", docId, e);
            fail(docId, FAILED, e);
        }
    }

    private DraftInput draftInput(Long offerId, DraftRequest req) {
        Offer offer = offerRepository.findById(offerId).orElseThrow();
        String clientType = "RETURNING".equals(req.clientType()) ? "RETURNING" : "NEW";
        List<String> groups = new ArrayList<>(scopeOf(req));
        if (!groups.contains(OTHER)) groups.add(OTHER);
        Person seller = offer.getUser() == null ? null : new Person(offer.getUser().getFullname(), "Opiekun handlowy",
                offer.getUser().getAvatar(), offer.getUser().getEmail(),
                offer.getUser().getPhone() == null ? null : String.valueOf(offer.getUser().getPhone()), List.of());
        return new DraftInput(userPrompt(offer, req, clientType, groups), groups, clientType,
                blank(req.projectName()) ? offer.getName() : req.projectName(), clientName(offer), offer.getId(), seller);
    }

    private static List<String> scopeOf(DraftRequest req) {
        return cleanScope(req.scope());
    }

    /** Odpowiedź bez kluczowych sekcji = błąd, a nie pusty szkic. */
    static void requireComplete(JsonNode ai) {
        if (ai == null || !ai.isObject() || ai.path("headline").asText("").isBlank()
                || !ai.path("situation").isArray() || ai.path("situation").isEmpty()
                || !ai.path("groups").isArray() || ai.path("groups").isEmpty()) {
            throw new IllegalStateException("AI zwróciło niepełną odpowiedź. Spróbuj ponownie.");
        }
    }

    /** FAQ w kolejności wybranej przez handlowca, tylko aktywne, max 5. */
    private List<Qa> chosenFaqs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        Map<Long, OfferFaq> byId = new HashMap<>();
        faqRepository.findAllById(ids).forEach(f -> byId.put(f.getId(), f));
        return ids.stream().map(byId::get).filter(f -> f != null && f.isActive()).limit(5)
                .map(f -> new Qa(f.getQuestion(), f.getAnswer())).toList();
    }

    @Transactional
    public OfferDocument update(Long id, UpdateRequest req) {
        OfferDocument doc = get(id);
        if (!READY.equals(doc.getStatus())) throw new IllegalStateException("Dokument jest w trakcie przetwarzania.");
        if (req.content() == null) throw new IllegalArgumentException("Brak treści dokumentu.");
        Map<Long, String> itemGroups = req.itemGroups() != null ? req.itemGroups() : readGroups(doc);
        doc.setContent(write(prepare(req.content(), doc, itemGroups, req.content().date())));
        doc.setItemGroups(write(itemGroups));
        // treść się zmieniła — stary PDF już jej nie odpowiada; usuwamy go, żeby stary link z nieaktualną ceną nie działał
        minio.deleteByUrl(doc.getPdfUrl());
        doc.setPdfUrl(null);
        doc.setGeneratedAt(null);
        doc.setError(null);
        return doc;
    }

    /** Sprawdza braki i zleca render w tle. Kwoty liczone od nowa z aktualnych pozycji oferty. */
    @Transactional
    public OfferDocument generatePdf(Long id) {
        OfferDocument doc = get(id);
        if (!READY.equals(doc.getStatus())) throw new IllegalStateException("Dokument jest w trakcie przetwarzania.");
        if (hasMissing(read(doc.getContent()))) {
            throw new IllegalStateException("W treści zostały miejsca do uzupełnienia (zaznaczone na czerwono). Uzupełnij je przed wygenerowaniem PDF.");
        }
        doc.setStatus(RENDERING);
        doc.setError(null);
        afterCommit(() -> background.submit(() -> runPdf(id)));
        return doc;
    }

    private void runPdf(Long id) {
        try {
            OfferPdfModel model = tx.execute(s -> {
                OfferDocument doc = documentRepository.findById(id).orElseThrow();
                if (!RENDERING.equals(doc.getStatus())) return null;
                // kwoty i data okładki aktualne na moment generowania
                OfferPdfModel fresh = prepare(read(doc.getContent()), doc, readGroups(doc), today());
                doc.setContent(write(fresh));
                return fresh;
            });
            if (model == null) return;
            byte[] pdf = gotenberg.htmlToPdf(renderHtml(model));
            // UUID w nazwie: bucket jest publiczny, adres nie może dać się zgadnąć
            String url = minio.uploadBytes(pdf, "oferty/" + UUID.randomUUID() + ".pdf", "application/pdf");
            tx.executeWithoutResult(s -> {
                OfferDocument doc = documentRepository.findById(id).orElseThrow();
                if (!RENDERING.equals(doc.getStatus())) { minio.deleteByUrl(url); return; } // przerwane w międzyczasie
                minio.deleteByUrl(doc.getPdfUrl());
                doc.setPdfUrl(url);
                doc.setGeneratedAt(LocalDateTime.now());
                doc.setStatus(READY);
                doc.setError(null);
            });
        } catch (Exception e) {
            log.warn("Nie udało się wygenerować PDF dokumentu {}", id, e);
            fail(id, READY, e);
        }
    }

    private void fail(Long id, String status, Exception e) {
        String msg = e instanceof IllegalStateException && e.getMessage() != null ? e.getMessage() : "Błąd: " + e.getMessage();
        tx.executeWithoutResult(s -> documentRepository.findById(id).ifPresent(d -> {
            d.setStatus(status);
            d.setError(msg.length() > 990 ? msg.substring(0, 990) : msg);
        }));
    }

    /**
     * Jedyna droga treści do bazy i do PDF: kwoty liczone teraz z pozycji oferty (nigdy z JSON-a ani z frontendu),
     * puste pola i wiersze usunięte (Thymeleaf traktuje "" jako prawdę i rysowałby puste ramki),
     * zdjęcia tylko z zaufanych adresów (Chromium w Gotenbergu pobiera je z sieci wewnętrznej).
     */
    private OfferPdfModel prepare(OfferPdfModel c, OfferDocument doc, Map<Long, String> itemGroups, String date) {
        Offer offer = doc.getOffer();
        List<String> scope = cleanScope(c.scope());
        List<String> groups = new ArrayList<>(scope);
        if (!groups.contains(OTHER)) groups.add(OTHER);
        return new OfferPdfModel(doc.getClientType(), c.projectName(), c.clientName(), offer.getId(), date,
                safePerson(c.preparedBy()), scope, lines(c.priorities()), blankToNull(c.headline()), lines(c.situation()),
                blankToNull(c.goal()),
                c.schedule() == null ? List.of() : c.schedule().stream()
                        .filter(st -> !blank(st.period()) || !blank(st.title()) || !blank(st.description())).toList(),
                lines(c.needs()),
                c.optimization() == null ? List.of() : c.optimization().stream()
                        .filter(it -> !blank(it.title()) || !blank(it.description())).toList(),
                buildPricing(offer.getOfferItemList(), itemGroups, groups, currency(offer)),
                c.similar() == null ? List.of() : c.similar().stream()
                        .map(p -> new Project(p.title(), p.description(), safeImage(p.imageUrl()))).toList(),
                c.team() == null ? List.of() : c.team().stream().map(this::safePerson).toList(),
                c.faq(), c.referenceLetters(),
                c.attachments() == null ? List.of() : c.attachments().stream()
                        .filter(a -> a.url() != null && a.url().matches("(?i)https?://.+")).toList());
    }

    /** Znaczniki [uzupełnij] szukamy tylko w polach pisanych przez AI — tych samych, które edytor zaznacza na czerwono. */
    static boolean hasMissing(OfferPdfModel m) {
        List<String> texts = new ArrayList<>();
        texts.add(m.headline());
        texts.add(m.goal());
        if (m.situation() != null) texts.addAll(m.situation());
        if (m.needs() != null) texts.addAll(m.needs());
        if (m.schedule() != null) m.schedule().forEach(st -> { texts.add(st.period()); texts.add(st.title()); texts.add(st.description()); });
        if (m.optimization() != null) m.optimization().forEach(it -> { texts.add(it.title()); texts.add(it.description()); });
        return texts.stream().anyMatch(t -> t != null && MISSING.matcher(t).find());
    }

    private Person safePerson(Person p) {
        return p == null ? null : new Person(p.name(), p.role(), safeImage(p.photoUrl()), p.email(), p.phone(), lines(p.helps()));
    }

    /** Zdjęcia tylko ze strony WH PLUS albo z naszego S3 — inne adresy (np. wewnętrzne) wycinamy. */
    String safeImage(String url) {
        if (url == null) return null;
        return url.startsWith(WpContactClient.SITE + "/") || url.startsWith(minio.publicPrefix()) ? url : null;
    }

    static List<String> cleanScope(List<String> scope) {
        List<String> out = lines(scope).stream().map(String::trim).distinct().toList();
        return out.isEmpty() ? List.of(OTHER) : out;
    }

    private static List<String> lines(List<String> in) {
        return in == null ? List.of() : in.stream().filter(x -> !blank(x)).toList();
    }

    private static String blankToNull(String s) { return blank(s) ? null : s; }

    private static String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("pl")));
    }

    private static void afterCommit(Runnable r) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { r.run(); }
            });
        } else {
            r.run();
        }
    }

    public String renderHtml(OfferPdfModel model) {
        Context ctx = new Context(new Locale("pl"));
        ctx.setVariable("doc", model);
        return templateEngine.process(model.isNewClient() ? "offer-pdf/new" : "offer-pdf/returning", ctx);
    }

    // ---------- FAQ ----------
    public List<OfferFaq> faqs() { return faqRepository.findAllByOrderByPositionAscIdAsc(); }

    @Transactional
    public OfferFaq saveFaq(Long id, OfferFaq in) {
        if (blank(in.getQuestion()) || blank(in.getAnswer())) throw new IllegalArgumentException("Pytanie i odpowiedź są wymagane.");
        OfferFaq f = id == null ? new OfferFaq() : faqRepository.findById(id).orElseThrow();
        f.setQuestion(in.getQuestion().trim());
        f.setAnswer(in.getAnswer().trim());
        f.setPosition(in.getPosition());
        f.setActive(in.isActive());
        return faqRepository.save(f);
    }

    public void deleteFaq(Long id) { faqRepository.deleteById(id); }

    // ---------- kwoty: zawsze z pozycji oferty, nigdy z AI ----------
    static Pricing buildPricing(List<OfferItem> items, Map<Long, String> itemGroups, List<String> groupOrder, String currency) {
        Map<String, List<Line>> lines = new LinkedHashMap<>();
        Map<String, BigDecimal[]> sums = new LinkedHashMap<>();
        groupOrder.forEach(g -> { lines.put(g, new ArrayList<>()); sums.put(g, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}); });
        BigDecimal totalNet = BigDecimal.ZERO, totalGross = BigDecimal.ZERO;
        for (OfferItem it : items) {
            if (it.getAmount() == null || it.getQuantity() == null) continue; // jak OfferService: niepełne pozycje pomijamy
            BigDecimal net = it.getAmount().multiply(BigDecimal.valueOf(it.getQuantity()));
            BigDecimal gross = it.getGrossAmount() != null ? it.getGrossAmount()
                    : it.getTax() != null ? net.add(net.multiply(BigDecimal.valueOf(it.getTax().getRate()))) : net;
            String g = itemGroups.getOrDefault(it.getId(), OTHER);
            if (!lines.containsKey(g)) g = OTHER;
            lines.computeIfAbsent(g, k -> new ArrayList<>()).add(new Line(it.getTitle(), it.getQuantity(), net.setScale(2, RoundingMode.HALF_UP)));
            BigDecimal[] s = sums.computeIfAbsent(g, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            s[0] = s[0].add(net);
            s[1] = s[1].add(gross);
            totalNet = totalNet.add(net);
            totalGross = totalGross.add(gross);
        }
        List<Group> groups = new ArrayList<>();
        lines.forEach((name, l) -> {
            if (l.isEmpty()) return;
            BigDecimal[] s = sums.get(name);
            groups.add(new Group(name, s[0].setScale(2, RoundingMode.HALF_UP), s[1].setScale(2, RoundingMode.HALF_UP), l));
        });
        return new Pricing(currency, totalNet.setScale(2, RoundingMode.HALF_UP), totalGross.setScale(2, RoundingMode.HALF_UP), groups);
    }

    // ---------- prompt ----------
    private static final String SYSTEM = """
            Piszesz treść oferty handlowej WH PLUS (27 lat na rynku, ponad 20 000 wyposażonych pokoi hotelowych, \
            własny dział projektowy i produkcja). Tekst ma brzmieć jak napisany przez doświadczonego handlowca, \
            który zna klienta, a nie przez AI ani dział marketingu. Wzór tonu (prawdziwa oferta):
            „Chcecie zakupić wyposażenie do 116 pokoi w nowo powstającym hotelu.”
            „Martwi Was jakość łóżek (rozstrzygniemy to na pokoju wzorcowym).”
            „Wpłaty zaliczki 30% w ciągu 7 dni od podpisania umowy.”

            Styl:
            - Krótkie, rzeczowe zdania. Jedna myśl w punkcie, maksymalnie ok. 20 słów.
            - Zwracasz się do klienta w 2. osobie liczby mnogiej (Chcecie, Wasz).
            - Bez przymiotników sprzedażowych i ozdobników: nie używaj słów takich jak kompleksowy, innowacyjny, \
              dedykowany, unikalny, najwyższa jakość, z przyjemnością, partnerstwo, synergia, holistyczny, \
              doskonały, wyjątkowy, ponadczasowy, „od A do Z”, „szyte na miarę”.
            - Bez wyliczeń trójkowych dla efektu, bez pytań retorycznych, bez wykrzykników, bez myślników pauzy (—).
            - Nie chwal firmy w treści (o firmie jest osobna, stała sekcja).

            Zasada nadrzędna: używasz WYŁĄCZNIE faktów z danych wejściowych. Nie wymyślasz liczb pokoi, terminów, \
            kwot, nazw, materiałów ani historii współpracy. Jeśli do sensownego zdania brakuje faktu, wstaw w tym miejscu \
            znacznik dokładnie w formie [uzupełnij: czego brakuje], handlowiec go uzupełni. Lepszy znacznik niż zgadywanie.

            Sekcje:
            - headline: rzeczowy podtytuł okładki, co i w jakiej skali (np. „Wyposażenie 116 pokoi, etapami od marca do maja”), \
              bez haseł reklamowych.
            - situation: 3-4 punkty „Jak rozumiemy Waszą sytuację”: czego klient chce, co jest dla niego ważne, \
              czego się obawia lub czego jeszcze nie wie.
            - goal: jedno zdanie: co, w jakim zakresie, w jakim czasie (czas tylko jeśli jest w danych).
            - schedule: 3-4 kroki (period: okres lub „Krok 1” gdy brak dat; title: 2-4 słowa; description: jedno zdanie). \
              Typowe etapy: specyfikacja ilościowo-cenowa, pokój wzorcowy, produkcja, montaż etapami.
            - needs: 3-5 krótkich punktów „Czego potrzebujemy od Was”: rzuty, akceptacje, zaliczka, logistyka, dostęp do pomieszczeń.
            - optimization: 2-3 konkretne propozycje pod priorytety klienta (title: kilka słów, description: jedno zdanie), bez kwot.
            - groups: przypisz KAŻDĄ pozycję oferty (itemId) do dokładnie jednej z podanych grup zakresu.
            Klient powracający: nawiąż do dotychczasowej współpracy tylko na podstawie podanej historii ofert.

            Źródła faktów: opis oferty, dane klienta, zapytanie (opis, liczba pokoi, termin), pozycje oferty z opisami, \
            uwagi handlowca i notatki zespołu. Notatki zespołu są wewnętrzne: wyciągaj z nich potrzeby, obawy i ustalenia, \
            ale ich nie cytuj i nie przenoś do tekstu opinii o kliencie ani informacji o cenach z negocjacji.
            """;

    private String userPrompt(Offer offer, DraftRequest req, String clientType, List<String> groups) {
        ObjectNode in = objectMapper.createObjectNode();
        in.put("typKlienta", "RETURNING".equals(clientType) ? "powracający" : "nowy");
        in.put("klient", clientName(offer));
        in.put("nazwaProjektu", blank(req.projectName()) ? offer.getName() : req.projectName());
        in.put("opisOferty", offer.getDescription());
        in.put("typObiektu", offer.getObjectType() == null ? null : offer.getObjectType().name());
        in.put("typInwestora", offer.getInvestorType() == null ? null : offer.getInvestorType().name());
        in.set("zakres", objectMapper.valueToTree(req.scope()));
        in.set("coNajwazniejsze", objectMapper.valueToTree(req.priorities()));
        in.put("uwagiHandlowca", req.notes());
        in.put("szansaSprzedazy", offer.getSalesOpportunityLevel() == null ? null : offer.getSalesOpportunityLevel().name());
        if (offer.getClient() != null) {
            var cl = offer.getClient();
            in.putObject("daneKlienta").put("firma", cl.getClientBusinessName()).put("osoba", cl.getClientFullName())
                    .put("miasto", cl.getClientCity()).put("kraj", cl.getClientCountry()).put("notatki", cl.getClientNotes());
        }
        if (offer.getLead() != null) {
            var ld = offer.getLead();
            in.putObject("zapytanie").put("nazwa", ld.getName()).put("opis", ld.getDescription())
                    .put("liczbaPokoi", ld.getRoomsQuantity())
                    .put("terminRealizacji", ld.getExecutionDate() == null ? null : ld.getExecutionDate().toLocalDate().toString())
                    .put("miasto", ld.getClientCity()).put("kraj", ld.getClientCountry())
                    .put("zrodlo", ld.getLeadSource() == null ? null : ld.getLeadSource().getName());
        }
        List<Comment> notes = new ArrayList<>(commentRepository.findTop15ByOfferIdAndAutomaticFalseOrderByIdDesc(offer.getId()));
        if (offer.getLead() != null) notes.addAll(commentRepository.findTop15ByLeadIdAndAutomaticFalseOrderByIdDesc(offer.getLead().getId()));
        if (offer.getClient() != null) notes.addAll(commentRepository.findTop15ByClientIdAndAutomaticFalseOrderByIdDesc(offer.getClient().getId()));
        ArrayNode notesNode = in.putArray("notatkiZespolu");
        notes.stream().filter(n -> !blank(n.getContent())).map(n -> n.getContent().strip()).distinct().limit(30)
                .forEach(n -> notesNode.add(n.length() > 600 ? n.substring(0, 600) + "…" : n));
        in.set("grupyDoPrzypisania", objectMapper.valueToTree(groups));
        ArrayNode items = in.putArray("pozycjeOferty");
        for (OfferItem it : offer.getOfferItemList()) {
            items.addObject().put("itemId", it.getId()).put("nazwa", it.getTitle()).put("opis", it.getDescription())
                    .put("ilosc", it.getQuantity());
        }
        if (req.similar() != null) in.set("podobneRealizacje", objectMapper.valueToTree(req.similar().stream().map(Project::title).toList()));
        if ("RETURNING".equals(clientType) && offer.getClient() != null) {
            ArrayNode hist = in.putArray("historiaOfertKlienta");
            offerRepository.findByClientId(offer.getClient().getId(), PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "id"))).forEach(o -> {
                if (!Objects.equals(o.getId(), offer.getId())) {
                    ObjectNode h = hist.addObject().put("nazwa", o.getName())
                            .put("status", o.getOfferStatus() == null ? null : o.getOfferStatus().name())
                            .put("umowaPodpisana", o.isContractSigned())
                            .put("dataPodpisania", o.getSignedContractDate() == null ? null : o.getSignedContractDate().toLocalDate().toString())
                            .put("typObiektu", o.getObjectType() == null ? null : o.getObjectType().name());
                    if (o.isContractSigned() && o.getOfferItemList() != null) {
                        h.set("produkty", objectMapper.valueToTree(o.getOfferItemList().stream().map(OfferItem::getTitle).limit(10).toList()));
                    }
                }
            });
        }
        try {
            return "Dane do oferty (JSON):\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(in);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode schema(List<String> groups) {
        ObjectNode s = obj();
        ObjectNode p = s.putObject("properties");
        p.set("headline", str());
        p.set("situation", arr(str()));
        p.set("goal", str());
        p.set("schedule", arr(objOf("period", "title", "description")));
        p.set("needs", arr(str()));
        p.set("optimization", arr(objOf("title", "description")));
        ObjectNode g = obj();
        g.putObject("properties").set("itemId", objectMapper.createObjectNode().put("type", "integer"));
        ((ObjectNode) g.get("properties")).set("group", objectMapper.createObjectNode().put("type", "string")
                .set("enum", objectMapper.valueToTree(groups)));
        g.set("required", objectMapper.valueToTree(List.of("itemId", "group")));
        p.set("groups", arr(g));
        s.set("required", objectMapper.valueToTree(List.of("headline", "situation", "goal", "schedule", "needs", "optimization", "groups")));
        return s;
    }

    private ObjectNode obj() { ObjectNode o = objectMapper.createObjectNode().put("type", "object"); o.put("additionalProperties", false); return o; }
    private ObjectNode str() { return objectMapper.createObjectNode().put("type", "string"); }
    private ObjectNode arr(ObjectNode items) { ObjectNode a = objectMapper.createObjectNode().put("type", "array"); a.set("items", items); return a; }
    private ObjectNode objOf(String... fields) {
        ObjectNode o = obj();
        ObjectNode props = o.putObject("properties");
        for (String f : fields) props.set(f, str());
        o.set("required", objectMapper.valueToTree(fields));
        return o;
    }

    // ---------- pomocnicze ----------
    /** shortcut: listy referencyjne są stałe; gdy będzie ich więcej lub mają być wybierane, przenieść do bazy. */
    private static final List<String> REFERENCE_LETTERS = List.of();

    private static String clientName(Offer o) {
        if (o.getClient() != null && !blank(o.getClient().getClientBusinessName())) return o.getClient().getClientBusinessName();
        if (o.getLead() != null && !blank(o.getLead().getClientBusinessName())) return o.getLead().getClientBusinessName();
        return o.getName();
    }

    private static String currency(Offer o) { return o.getCurrency() == null ? "PLN" : o.getCurrency().name(); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private List<String> strings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        arr.forEach(n -> out.add(n.asText()));
        return out;
    }

    private <T> T convert(JsonNode n, TypeReference<T> t) { return objectMapper.convertValue(n, t); }

    private String write(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private OfferPdfModel read(String json) {
        try { return objectMapper.readValue(json, OfferPdfModel.class); } catch (Exception e) { throw new IllegalStateException("Uszkodzona treść dokumentu.", e); }
    }

    private Map<Long, String> readGroups(OfferDocument d) {
        try {
            return d.getItemGroups() == null ? new HashMap<>() : objectMapper.readValue(d.getItemGroups(), new TypeReference<Map<Long, String>>() {});
        } catch (Exception e) { return new HashMap<>(); }
    }
}
