package wh.plus.crm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import wh.plus.crm.model.Currency;
import wh.plus.crm.model.Role;
import wh.plus.crm.model.Tax;
import wh.plus.crm.model.client.Client;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.model.offer.*;
import wh.plus.crm.model.offer.document.OfferFaq;
import wh.plus.crm.model.user.SalesTeam;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Dane demo dla lokalnego środowiska (hotele, leady, oferty z pozycjami, FAQ).
 * Włączanie: profil dev + crm.demo-data=true (np. env CRM_DEMO_DATA=true). Działa tylko, gdy w bazie nie ma ofert.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "crm.demo-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "Demo1234!";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SalesTeamRepository teamRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final LeadStatusRepository statusRepository;
    private final LeadSourceRepository sourceRepository;
    private final OfferRepository offerRepository;
    private final OfferFaqRepository faqRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (offerRepository.count() > 0) {
            log.info("Demo data: w bazie są już oferty, pomijam.");
            return;
        }
        SalesTeam team = teamRepository.findAll().stream().filter(t -> "Zespół hotelowy".equals(t.getName())).findFirst()
                .orElseGet(() -> teamRepository.save(new SalesTeam(0, "Zespół hotelowy", new ArrayList<>())));

        User me = userRepository.findByUsername("marcinpohl").orElseThrow();
        me.setEmail("m.pohl@w-h.pl");
        me.setFullname("Marcin Pohl");
        me.setSalesRepresentative(true);
        me.setTeam(team);
        User anna = user("a.kowalska", "Anna Kowalska", "a.kowalska@w-h.pl", team, Role.RoleName.SALESPERSON);
        User tomasz = user("t.wisniewski", "Tomasz Wiśniewski", "t.wisniewski@w-h.pl", team, Role.RoleName.SALESPERSON);

        Client wislana = client("Hotel Wiślana Sp. z o.o.", "Katarzyna Nowak", "ul. Długie Pobrzeże 12", "Gdańsk", "80-888", "k.nowak@hotelwislana.pl", 501234567L, 5842311200L);
        Client baltyk = client("Grupa Hotelowa Bałtyk S.A.", "Piotr Zieliński", "ul. Sułkowskiego 3", "Kołobrzeg", "78-100", "p.zielinski@baltykhotele.pl", 602345678L, 6711807730L);
        Client mariacki = client("Aparthotel Mariacki", "Joanna Lewandowska", "ul. Floriańska 40", "Kraków", "31-019", "biuro@aparthotelmariacki.pl", 703456789L, 6762457811L);
        Client sniezka = client("Resort Pod Śnieżką Sp. z o.o.", "Michał Wójcik", "ul. Konstytucji 3 Maja 50", "Karpacz", "58-540", "inwestycja@resortpodsniezka.pl", 604567890L, 6112789044L);
        Client forma = client("Biuro Projektowe Forma", "Agnieszka Kamińska", "ul. Mokotowska 21", "Warszawa", "00-560", "a.kaminska@forma-studio.pl", 505678901L, 5213365921L);
        Client warta = client("Hotel Nad Wartą", "Robert Dąbrowski", "ul. Garbary 77", "Poznań", "61-758", "r.dabrowski@hotelnadwarta.pl", 606789012L, 7792456630L);

        // --- leady ---
        Lead lWislana = lead("Hotel Wiślana — wyposażenie 116 pokoi", wislana, me, "QUOTATION_SENT", "DIRECT_EMAIL", wh.plus.crm.model.lead.ClientType.PRIVATE_CLIENT, 116L, 2_400_000d, 40,
                "Nowo budowany hotel 4* na Ołowiance. Otwarcie planowane na czerwiec. Inwestor zna nas z realizacji w Sopocie.");
        Lead lBaltyk = lead("Bałtyk Kołobrzeg — renowacja skrzydła B", baltyk, anna, "CLIENT", "RETURNING_CLIENT", wh.plus.crm.model.lead.ClientType.HOTEL_CHAIN, 64L, 1_100_000d, 120,
                "Wymiana mebli w 64 pokojach skrzydła B, w standardzie skrzydła A z 2023 r.");
        Lead lBaltyk2 = lead("Bałtyk Mielno — restauracja i lobby", baltyk, anna, "IN_QUOTATION", "RETURNING_CLIENT", wh.plus.crm.model.lead.ClientType.HOTEL_CHAIN, 0L, 450_000d, 14,
                "Restauracja na 120 miejsc i lobby bar. Klient chce kontynuacji standardu z Kołobrzegu.");
        Lead lMariacki = lead("Aparthotel Mariacki — 28 apartamentów", mariacki, tomasz, "QUOTATION_SENT", "WEBSITE_FORM", wh.plus.crm.model.lead.ClientType.PRIVATE_CLIENT, 28L, 620_000d, 25,
                "Kamienica na Starym Mieście, apartamenty na wynajem krótkoterminowy. Ważny budżet i trwałość.");
        Lead lSniezka = lead("Resort Pod Śnieżką — pokoje i SPA", sniezka, me, "IN_VERIFICATION", "TRADE_SHOW", wh.plus.crm.model.lead.ClientType.CONSTRUCTION_COMPANY, 180L, 3_600_000d, 6,
                "Kontakt z targów Hotel Tech. Generalny wykonawca szuka dostawcy wyposażenia pod klucz.");
        Lead lForma = lead("Forma — hotel butikowy Sopot (projekt)", forma, tomasz, "IN_QUOTATION", "DESIGN_OFFICE", wh.plus.crm.model.lead.ClientType.DESIGN_OFFICE, 42L, 900_000d, 10,
                "Biuro projektowe ma gotowy projekt wnętrz, szuka wykonawcy mebli na wymiar.");
        Lead lWarta = lead("Hotel Nad Wartą — sala konferencyjna", warta, anna, "REJECTED", "PHONE_CALL", wh.plus.crm.model.lead.ClientType.PRIVATE_CLIENT, 0L, 180_000d, 60,
                "Meble do sali konferencyjnej na 80 osób.");
        lead("Pensjonat Jodłowy Dwór — 20 pokoi", null, tomasz, "NEW", "SEO", wh.plus.crm.model.lead.ClientType.PRIVATE_CLIENT, 20L, 300_000d, 1,
                "Zapytanie z formularza: wymiana łóżek i szaf w 20 pokojach.");
        lead("Apartamenty Marina Gdynia", null, me, "CONTACT_IN_FUTURE", "LINKEDIN", wh.plus.crm.model.lead.ClientType.CONSTRUCTION_COMPANY, 54L, 800_000d, 30,
                "Deweloper wróci do tematu po uzyskaniu pozwolenia na użytkowanie.");
        lead("Hotel Leśny Zakątek — modernizacja", null, anna, "NEW", "GOOGLE_ADS", wh.plus.crm.model.lead.ClientType.PRIVATE_CLIENT, 35L, 500_000d, 2,
                "Modernizacja pokoi w hotelu z lat 90.");

        // --- oferty ---
        offer("Hotel Wiślana — wyposażenie pokoi", wislana, lWislana, me, team, OfferStatus.SENT, ClientType.NEW_CLIENT, ObjectType.INDEPENDENT_HOTELS,
                InvestorType.LARGE_PRIVATE_INVESTOR, SalesOpportunityLevel.HIGH, 30,
                "Wyposażenie 116 pokoi, etapami od marca do maja. Pokój wzorcowy do końca stycznia.",
                item("Pokój standard — komplet mebli", "Łóżko z zagłówkiem tapicerowanym, szafa z garderobą, biurko, konsola TV", 96, "9200"),
                item("Pokój typu suite — komplet mebli", "Sofa rozkładana, stolik kawowy, rozszerzona garderoba", 20, "15800"),
                item("Restauracja — krzesła i stoły", "64 krzesła tapicerowane, 16 stołów z blatem HPL", 1, "184250"),
                item("Lobby — recepcja i strefa wypoczynku", "Lada recepcyjna z konglomeratu, 2 sofy, 6 foteli", 1, "114000"),
                item("Montaż i transport", "Montaż piętrami, transport z zakładu w Kępnie", 1, "86000"));
        offer("Bałtyk Kołobrzeg — skrzydło A (2023)", baltyk, null, anna, team, OfferStatus.SIGNED, ClientType.NEW_CLIENT, ObjectType.CHAIN_HOTELS,
                InvestorType.MULTI_PROPERTY_PRIVATE_INVESTOR, SalesOpportunityLevel.HIGH, 700,
                "Pierwsza realizacja dla Grupy Bałtyk: 72 pokoje skrzydła A.",
                item("Pokój standard — komplet mebli", "Dąb naturalny, szafa przesuwna", 72, "8100"),
                item("Montaż", "Montaż w dwóch etapach", 1, "54000"));
        offer("Bałtyk Kołobrzeg — skrzydło B", baltyk, lBaltyk, anna, team, OfferStatus.ACCEPTED, ClientType.RETURNING_CLIENT, ObjectType.CHAIN_HOTELS,
                InvestorType.MULTI_PROPERTY_PRIVATE_INVESTOR, SalesOpportunityLevel.HIGH, 90,
                "Wymiana mebli w 64 pokojach w standardzie skrzydła A.",
                item("Pokój standard — komplet mebli (standard skrzydła A)", "Dąb naturalny, szafa przesuwna, nowe zagłówki", 58, "8650"),
                item("Pokój rodzinny — komplet mebli", "Dodatkowe łóżko piętrowe, większa szafa", 6, "11900"),
                item("Korytarze — konsole i lustra", "12 konsol z lustrem", 12, "2400"),
                item("Demontaż starych mebli i montaż", "Praca etapami, bez zamykania hotelu", 1, "62000"));
        offer("Bałtyk Mielno — restauracja i lobby", baltyk, lBaltyk2, anna, team, OfferStatus.DRAFT, ClientType.RETURNING_CLIENT, ObjectType.CHAIN_HOTELS,
                InvestorType.MULTI_PROPERTY_PRIVATE_INVESTOR, SalesOpportunityLevel.MODERATE, 5,
                "Restauracja na 120 miejsc i lobby bar.",
                item("Restauracja — krzesła", "Krzesło tapicerowane, rama dębowa", 120, "890"),
                item("Restauracja — stoły", "Stoły 2- i 4-osobowe, blat HPL", 34, "1650"),
                item("Lobby bar — lada i hokery", "Lada z frontem lamelowym, 10 hokerów", 1, "68000"),
                item("Lobby — strefa wypoczynku", "3 sofy, 8 foteli, stoliki", 1, "52000"));
        offer("Aparthotel Mariacki — 28 apartamentów", mariacki, lMariacki, tomasz, team, OfferStatus.SENT, ClientType.NEW_CLIENT, ObjectType.APARTMENTS,
                InvestorType.SMALL_PRIVATE_INVESTOR, SalesOpportunityLevel.MODERATE, 18,
                "Apartamenty na wynajem krótkoterminowy, aneksy kuchenne w zakresie.",
                item("Apartament — sypialnia", "Łóżko 160, szafa, szafki nocne", 28, "7400"),
                item("Apartament — aneks kuchenny", "Zabudowa kuchenna z AGD do zabudowy", 28, "9800"),
                item("Apartament — salon", "Sofa rozkładana, stolik, komoda RTV", 28, "5200"));
        offer("Resort Pod Śnieżką — wstępna wycena", sniezka, lSniezka, me, team, OfferStatus.DRAFT, ClientType.NEW_CLIENT, ObjectType.LARGE_HOTELS,
                InvestorType.CONSTRUCTION_COMPANY_HOTEL, SalesOpportunityLevel.LOW, 3,
                "Wycena orientacyjna na podstawie rzutów, bez specyfikacji materiałowej.",
                item("Pokój standard — komplet mebli", "Wycena orientacyjna", 150, "8800"),
                item("Pokój typu suite — komplet mebli", "Wycena orientacyjna", 30, "16500"),
                item("Strefa SPA — leżanki i szafki", "Leżanki, szafki na ubrania, recepcja SPA", 1, "210000"));
        offer("Forma — hotel butikowy Sopot", forma, lForma, tomasz, team, OfferStatus.DRAFT, ClientType.NEW_CLIENT, ObjectType.INDEPENDENT_HOTELS,
                InvestorType.LARGE_PRIVATE_INVESTOR, SalesOpportunityLevel.MODERATE, 7,
                "Meble na wymiar według projektu biura Forma.",
                item("Pokój — meble na wymiar wg projektu", "Fornir orzech, okucia Blum", 42, "13400"),
                item("Restauracja — ławy i stoły na wymiar", "Ławy tapicerowane wzdłuż ścian", 1, "96000"));
        offer("Hotel Nad Wartą — sala konferencyjna", warta, lWarta, anna, team, OfferStatus.REJECTED, ClientType.NEW_CLIENT, ObjectType.INDEPENDENT_HOTELS,
                InvestorType.SMALL_PRIVATE_INVESTOR, SalesOpportunityLevel.LOW, 55,
                "Meble do sali konferencyjnej na 80 osób.",
                item("Krzesła konferencyjne sztaplowane", "Tapicerka, stelaż chrom", 80, "720"),
                item("Stoły składane", "Blat 160x80, krawędź ABS", 40, "980"));

        faqs();
        log.info("Demo data: dodano klientów, leady, oferty i FAQ. Konta handlowców: a.kowalska / t.wisniewski, hasło {}", DEMO_PASSWORD);
    }

    private User user(String username, String name, String email, SalesTeam team, Role.RoleName role) {
        Optional<User> existing = userRepository.findByUsername(username); // ponowny seed po wyczyszczeniu samych ofert
        if (existing.isPresent()) return existing.get();
        User u = new User();
        u.setUsername(username);
        u.setFullname(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        u.setSalesRepresentative(true);
        u.setTeam(team);
        u.setRoles(new HashSet<>(Set.of(roleRepository.findByName(role).orElseThrow())));
        return userRepository.save(u);
    }

    private Client client(String business, String person, String address, String city, String zip, String email, Long phone, Long vat) {
        Client c = new Client();
        c.setClientBusinessName(business);
        c.setClientFullName(person);
        c.setClientAdress(address);
        c.setClientCity(city);
        c.setClientZip(zip);
        c.setClientCountry("Polska");
        c.setClientEmail(email);
        c.setClientPhone(phone);
        c.setVatNumber(vat);
        return clientRepository.save(c);
    }

    private Lead lead(String name, Client c, User owner, String status, String source, wh.plus.crm.model.lead.ClientType type,
                      Long rooms, Double value, int daysAgo, String description) {
        LeadStatus st = statusRepository.findByStatusName(status).orElseThrow();
        LeadSource src = sourceRepository.findByName(source).orElseThrow();
        Lead l = new Lead();
        l.setName(name);
        l.setUser(owner);
        l.setLeadStatus(st);
        l.setLeadSource(src);
        l.setFinal(st.isFinal());
        l.setClientType(type);
        l.setRoomsQuantity(rooms);
        l.setLeadValue(value);
        l.setDescription(description);
        l.setReturningClient("RETURNING_CLIENT".equals(source));
        l.setExecutionDate(LocalDateTime.now().plusMonths(4));
        l.setCreationDate(LocalDateTime.now().minusDays(daysAgo));
        if (c != null) {
            l.setClientBusinessName(c.getClientBusinessName());
            l.setClientFullName(c.getClientFullName());
            l.setClientAdress(c.getClientAdress());
            l.setClientCity(c.getClientCity());
            l.setClientZip(c.getClientZip());
            l.setClientCountry(c.getClientCountry());
            l.setClientEmail(c.getClientEmail());
            l.setClientPhone(c.getClientPhone());
            l.setVatNumber(c.getVatNumber());
        }
        return leadRepository.save(l);
    }

    private OfferItem item(String title, String description, long qty, String unitNet) {
        OfferItem i = new OfferItem();
        i.setTitle(title);
        i.setDescription(description);
        i.setQuantity(qty);
        i.setAmount(new BigDecimal(unitNet));
        i.setTax(Tax.VAT_23);
        // jak OfferService.calculateGrossAndTaxForItems
        BigDecimal net = i.getAmount().multiply(BigDecimal.valueOf(qty));
        i.setTaxAmount(net.multiply(BigDecimal.valueOf(Tax.VAT_23.getRate())));
        i.setGrossAmount(net.add(i.getTaxAmount()));
        return i;
    }

    private void offer(String name, Client c, Lead lead, User owner, SalesTeam team, OfferStatus status, ClientType clientType,
                       ObjectType objectType, InvestorType investorType, SalesOpportunityLevel chance, int daysAgo,
                       String description, OfferItem... items) {
        Offer o = new Offer();
        o.setName(name);
        o.setClient(c);
        o.setLead(lead);
        o.setUser(owner);
        o.setSalesTeam(team);
        o.setOfferStatus(status);
        o.setClientType(clientType);
        o.setObjectType(objectType);
        o.setInvestorType(investorType);
        o.setSalesOpportunityLevel(chance);
        o.setCurrency(Currency.PLN);
        o.setDescription(description);
        o.setCreationDate(LocalDateTime.now().minusDays(daysAgo));
        o.setStatusChangeDate(LocalDateTime.now().minusDays(Math.max(0, daysAgo - 3)));
        if (status == OfferStatus.SIGNED) {
            o.setContractSigned(true);
            o.setSignedContractDate(LocalDateTime.now().minusDays(daysAgo - 20));
        }
        if (status == OfferStatus.ACCEPTED || status == OfferStatus.REJECTED || status == OfferStatus.SIGNED) {
            o.setRejectionOrApprovalDate(LocalDateTime.now().minusDays(Math.max(0, daysAgo - 10)));
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OfferItem i : items) {
            i.setOffer(o);
            o.getOfferItemList().add(i);
            total = total.add(i.getAmount().multiply(BigDecimal.valueOf(i.getQuantity())));
        }
        o.setTotalPrice(total);
        offerRepository.save(o);
    }

    private void faqs() {
        if (faqRepository.count() > 0) return;
        String[][] qa = {
                {"Czy możemy zobaczyć meble przed zamówieniem całości?", "Tak. Wykonujemy pokój wzorcowy, na którym zatwierdzacie materiały, kolory i wygodę łóżek."},
                {"Kto montuje meble?", "Nasza własna ekipa montażowa. Nie korzystamy z podwykonawców."},
                {"Jaka jest gwarancja?", "Udzielamy 24 miesięcy gwarancji, a serwis pogwarancyjny realizujemy w ciągu 14 dni."},
                {"Czy możecie dostosować meble do istniejącego projektu architekta?", "Tak. Pracujemy na dokumentacji architekta i doprecyzowujemy detale produkcyjne."},
                {"Jakie są warunki płatności?", "Standardowo 30% zaliczki po podpisaniu umowy, reszta po montażu kolejnych etapów."},
                {"Ile trwa produkcja?", "Zwykle 8 do 12 tygodni od akceptacji pokoju wzorcowego, zależnie od skali."},
                {"Czy montaż może odbywać się przy działającym hotelu?", "Tak. Montujemy piętrami lub skrzydłami, uzgadniając harmonogram z obsługą hotelu."},
                {"Czy zajmujecie się demontażem starych mebli?", "Tak, na życzenie demontujemy i wywozimy stare wyposażenie."},
        };
        for (int i = 0; i < qa.length; i++) {
            OfferFaq f = new OfferFaq();
            f.setQuestion(qa[i][0]);
            f.setAnswer(qa[i][1]);
            f.setPosition(i);
            faqRepository.save(f);
        }
    }
}
