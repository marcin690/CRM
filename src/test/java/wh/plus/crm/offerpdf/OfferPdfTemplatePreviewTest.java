package wh.plus.crm.offerpdf;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import wh.plus.crm.dto.offer.OfferPdfModel;
import wh.plus.crm.dto.offer.OfferPdfModel.*;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regresja szablonów oferty PDF + podgląd: renderuje oba typy do target/offer-pdf-preview/
 * razem z assetami (obok HTML, tak jak u Gotenberga). PDF do obejrzenia: Chrome headless --print-to-pdf.
 */
class OfferPdfTemplatePreviewTest {

    private static final Path OUT = Paths.get("target/offer-pdf-preview");

    @Test
    void rendersBothTemplates() throws Exception {
        SpringTemplateEngine engine = engine();
        Files.createDirectories(OUT);
        copyDir(Paths.get("src/main/resources/offer-pdf-assets"));
        copyDir(Paths.get("src/test/resources/offer-pdf-sample"));

        for (String type : List.of("NEW", "RETURNING")) {
            String html = render(engine, sample(type));
            Files.writeString(OUT.resolve(type.equals("NEW") ? "new.html" : "returning.html"), html);
            assertTrue(html.contains("Hotel Otis Kraków"), "brak nazwy projektu");
            assertTrue(html.contains("1 284 650,00"), "zła kwota netto: " + type);
            assertEquals(type.equals("NEW"), html.contains("Listy referencyjne"), "listy referencyjne tylko dla NEW");
            assertEquals(type.equals("NEW"), html.contains("O WH PLUS"), "O WH PLUS tylko dla NEW");
            assertTrue(html.contains("Wycena pokoi.xlsx") && html.contains("href=\"https://crm.example/files/wycena-pokoi.xlsx\""), "brak linku do załącznika");
            assertTrue(html.contains("Dobierze materiały i dekory"), "brak opisu „W czym pomoże”");
            assertFalse(html.contains("Czwarty punkt"), "max 3 punkty „W czym pomoże”");
            assertTrue(html.contains(">77%<"), "zły udział grupy w sumie");
            assertTrue(html.contains("WH/1287/2"), "brak numeru wersji oferty");
            assertTrue(html.contains("19 grudnia 2026"), "brak daty ważności");
            assertTrue(html.contains("Szafa z garderobą") && html.contains("116 × 1 810,34"), "brak rozbicia na pozycje z ceną jednostkową");
        }
    }

    @Test
    void emptySectionsAreSkipped() {
        OfferPdfModel bare = new OfferPdfModel("NEW", "Projekt", "Klient", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        String html = render(engine(), bare);
        assertFalse(html.contains("w załącznikach"), "nota o załącznikach bez załączników");
        assertFalse(html.contains("Jaki mamy plan działania?"));
        assertFalse(html.contains(">Wycena<"));
        assertTrue(html.contains("Projekt"));
    }

    private static String render(SpringTemplateEngine engine, OfferPdfModel doc) {
        Context ctx = new Context();
        ctx.setVariable("doc", doc);
        return engine.process(doc.isNewClient() ? "offer-pdf/new" : "offer-pdf/returning", ctx);
    }

    private static SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver r = new ClassLoaderTemplateResolver();
        r.setPrefix("templates/");
        r.setSuffix(".html");
        r.setCharacterEncoding("UTF-8");
        SpringTemplateEngine e = new SpringTemplateEngine();
        e.setTemplateResolver(r);
        return e;
    }

    private static void copyDir(Path dir) throws Exception {
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.toList()) Files.copy(f, OUT.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Jak w produkcji: wartość = cena jednostkowa × ilość. */
    private static Line line(String title, long qty, String unit) {
        BigDecimal u = new BigDecimal(unit);
        return new Line(title, qty, u, u.multiply(BigDecimal.valueOf(qty)));
    }

    static OfferPdfModel sample(String type) {
        boolean isNew = type.equals("NEW");
        Person seller = new Person("Rafał Hyla", "Prezes Zarządu", "osoba-25.jpg", "r.hyla@w-h.pl", "+48 600 100 200",
                List.of("Ustali zakres i warunki umowy", "Odpowie na pytania o wycenę", "Dopilnuje terminów po naszej stronie"));
        return new OfferPdfModel(
                type, "Hotel Otis Kraków", "Otis Hotels Sp. z o.o.", 1287L, java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("pl"))), seller,
                List.of("Pokoje", "Restauracja", "Części wspólne"),
                isNew ? List.of("Optymalizacja kosztów", "Jakość", "Kompleksowa obsługa")
                      : List.of("Kontynuacja standardu wcześniejszych realizacji", "Optymalizacja kosztów"),
                isNew ? "Wyposażymy 116 pokoi etapami, piętro po piętrze, tak aby hotel mógł otworzyć się zgodnie z planem."
                      : "Kolejne 116 pokoi w standardzie, który Wasi goście już znają z poprzednich obiektów.",
                List.of("Chcecie zakupić wyposażenie do 116 pokoi w nowo powstającym hotelu.",
                        "Cena nie powinna być wyższa o więcej niż 50% od porównywalnych mebli kupowanych 4 lata temu, w podobnym kolorze płyty.",
                        "Martwi Was jakość łóżek. Rozstrzygniemy to na pokoju wzorcowym.",
                        "Nie wiecie jeszcze, jakie i ile sof będzie potrzebnych w częściach wspólnych."),
                "Wyposażenie i montaż 116 pokoi etapami, od początku marca do końca maja 2027, czyli w 3 miesiące.",
                List.of(new Step("Listopad 2026", "Specyfikacja ilościowo-cenowa", "Kończymy prace nad specyfikacją na podstawie rzutów pomieszczeń."),
                        new Step("Styczeń 2027", "Pokój wzorcowy", "Wykonujemy pokój wzorcowy, na którym wspólnie zatwierdzamy materiały, łóżka i detale."),
                        new Step("Luty 2027", "Produkcja", "Uruchamiamy produkcję pierwszych kondygnacji po podpisaniu umowy i wpłacie zaliczki."),
                        new Step("Marzec – maj 2027", "Montaż piętrami", "Realizujemy inwestycję piętrami, począwszy od pierwszego, przez 3 miesiące.")),
                List.of("Rzutów pomieszczeń do wykonania specyfikacji ilościowo-cenowej do końca listopada.",
                        "Akceptacji specyfikacji i podpisania umowy do końca stycznia.",
                        "Wpłaty zaliczki 30% w ciągu 7 dni od podpisania umowy.",
                        "Informacji o możliwości podjazdu samochodów ciężarowych na inwestycję.",
                        "Przygotowania pokoi do montażu kondygnacjami od początku marca."),
                List.of(new Item("Jeden dekor płyty w całym hotelu", "Ujednolicenie dekorów skraca produkcję i obniża koszt materiału o kilka procent."),
                        new Item("Łóżka w systemie modułowym", "Te same stelaże w pokojach typu double i twin ułatwiają serwis i wymianę."),
                        new Item("Sofy z jednej linii", "Jeden model w trzech szerokościach zamiast trzech osobnych modeli."),
                        new Item("Dostawy pod montaż pięter", "Dostarczamy meble partiami, więc nie potrzebujecie magazynu na budowie.")),
                new Pricing("PLN", new BigDecimal("1284650.00"), new BigDecimal("1580119.50"), List.of(
                        new Group("Pokoje hotelowe", new BigDecimal("986400.00"), new BigDecimal("1213272.00"), List.of(
                                line("Łóżko z zagłówkiem tapicerowanym", 116, "3275.86"),
                                line("Szafa z garderobą", 116, "1810.34"),
                                line("Biurko z konsolą TV", 116, "1293.10"),
                                line("Stolik nocny", 232, "387.93"),
                                line("Fotel wypoczynkowy", 116, "1348.28"))),
                        new Group("Restauracja", new BigDecimal("184250.00"), new BigDecimal("226627.50"), List.of(
                                line("Krzesło tapicerowane", 120, "800.00"),
                                line("Stół restauracyjny", 30, "2941.67"))),
                        new Group("Części wspólne", new BigDecimal("114000.00"), new BigDecimal("140220.00"), List.of(
                                line("Lada recepcyjna", 1, "64000.00"),
                                line("Sofy do lobby", 6, "8333.33"))))),
                List.of(new Project("Radisson Blu Kraków", "116 pokoi, lobby i restauracja. Projekt, produkcja i montaż w 10 tygodni.", "realizacja-1.jpg"),
                        new Project("Hotel Saski", "Sala konferencyjna i zabudowy ścienne.", "realizacja-2.jpg"),
                        new Project("Aparthotel Vistula", "Strefa wypoczynku i oświetlenie dekoracyjne.", "realizacja-3.jpg")),
                List.of(new Person("Dorota Przybyła", "Manager działu projektowego", "osoba-24.jpg", "d.przybyla@w-h.pl", "+48 600 100 201",
                                List.of("Dobierze materiały i dekory", "Przygotuje rysunki wykonawcze", "Poprowadzi pokój wzorcowy", "Czwarty punkt")),
                        new Person("Vasyl Kozar", "Szef ekipy montażowej", "osoba-23.jpg", null, "+48 600 100 202",
                                List.of("Zaplanuje montaż piętrami", "Odbierze z Wami gotowe pokoje")),
                        new Person("Justyna Chociaj", "Dział finansów i logistyki", "osoba-26.jpg", "j.chociaj@w-h.pl", null, null),
                        seller),
                List.of(new Qa("Czy możemy zobaczyć meble przed zamówieniem całości?", "Tak. Wykonujemy pokój wzorcowy, na którym zatwierdzacie materiały, kolory i wygodę łóżek."),
                        new Qa("Kto montuje meble?", "Nasza własna ekipa montażowa. Nie korzystamy z podwykonawców."),
                        new Qa("Jak wygląda gwarancja?", "Udzielamy 24 miesięcy gwarancji, a serwis pogwarancyjny realizujemy w ciągu 14 dni."),
                        new Qa("Czy możecie dostosować meble do istniejącego projektu architekta?", "Tak. Pracujemy na dokumentacji architekta i doprecyzowujemy detale produkcyjne."),
                        new Qa("Jakie są warunki płatności?", "Standardowo 30% zaliczki po podpisaniu umowy, reszta po montażu kolejnych etapów.")),
                isNew ? List.of("list-1.jpg", "list-2.jpg", "list-1.jpg") : List.of(),
                List.of(new Attachment("Wycena pokoi.xlsx", "https://crm.example/files/wycena-pokoi.xlsx"),
                        new Attachment("Wycena restauracji i części wspólnych.xlsx", "https://crm.example/files/wycena-restauracja.xlsx")),
                "WH/1287/2", "2026-12-19"
        );
    }
}
