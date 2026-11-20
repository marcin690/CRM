package wh.plus.crm.dto.offer;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wszystko, co szablon PDF oferty (templates/offer-pdf/*.html) dostaje jako zmienną "doc".
 * Teksty pisze AI (Claude), kwoty liczy kod z pozycji oferty, reszta to wybory handlowca.
 */
public record OfferPdfModel(
        String clientType,              // NEW | RETURNING (wybiera szablon)
        String projectName,
        String clientName,
        Long offerId,
        String date,                    // np. "17 listopada 2026"
        Person preparedBy,
        List<String> scope,             // Pokoje, Restauracja, Części wspólne, inne
        List<String> priorities,        // co najważniejsze dla klienta
        String headline,                // AI: jedno mocne zdanie na okładkę/intro
        List<String> situation,         // AI: "Jak rozumiemy Waszą sytuację"
        String goal,                    // AI: "Jaki mamy cel?"
        List<Step> schedule,            // AI: proponowany harmonogram
        List<String> needs,             // AI: "Czego potrzebujemy od Was"
        List<Item> optimization,        // AI: propozycja optymalizacji
        Pricing pricing,                // kod: grupy i sumy z pozycji oferty
        List<Project> similar,          // wybrane realizacje (WordPress)
        List<Person> team,              // opiekunowie projektu
        List<Qa> faq,                   // max 5 z listy FAQ
        List<String> referenceLetters,  // URL-e skanów listów referencyjnych (tylko NEW)
        List<Attachment> attachments,   // pliki ze szczegółowym wyliczeniem (Excel itp.) dołączone w kreatorze
        String offerNumber,             // kod: WH/{id oferty}/{kolejny PDF tej oferty}, żeby klient mógł wskazać wersję
        String validUntil               // handlowiec: data ważności oferty, ISO yyyy-MM-dd
) {
    public record Person(String name, String role, String photoUrl, String email, String phone,
                         List<String> helps) {} // helps: "W czym pomoże" z WordPressa
    public record Step(String period, String title, String description) {}
    public record Item(String title, String description) {}
    public record Project(String title, String description, String imageUrl) {}
    public record Attachment(String name, String url) {}
    public record Qa(String question, String answer) {}
    public record Line(String title, Long quantity, BigDecimal unitNet, BigDecimal net) {}
    public record Group(String name, BigDecimal net, BigDecimal gross, List<Line> lines) {}
    public record Pricing(String currency, BigDecimal totalNet, BigDecimal totalGross, List<Group> groups) {}

    public boolean isNewClient() { return "NEW".equals(clientType); }

    /** Data ważności po polsku do szablonu ("19 grudnia 2026"); błędna lub pusta = brak. */
    public String validUntilText() {
        try {
            return validUntil == null ? null : java.time.LocalDate.parse(validUntil)
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("pl")));
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}
