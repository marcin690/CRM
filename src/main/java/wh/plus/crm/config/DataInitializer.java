package wh.plus.crm.config;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import wh.plus.crm.model.Role;
import wh.plus.crm.model.user.User;
import wh.plus.crm.model.lead.LeadSource;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.repository.LeadSourceRepository;
import wh.plus.crm.repository.LeadStatusRepository;
import wh.plus.crm.repository.RoleRepository;
import wh.plus.crm.model.lead.LeadStatus.StatusType;
import wh.plus.crm.repository.UserRepository;
import wh.plus.crm.service.LeadFactoryService;
import wh.plus.crm.service.OfferFactoryService;


import java.util.*;

@Component
@AllArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final LeadStatusRepository leadStatusRepository;
    private final LeadSourceRepository leadSourceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeadFactoryService leadFactoryService;
    private final OfferFactoryService offerFactoryService;

    @PostConstruct
    @Transactional
    public void initialize() {
        initializeRoles();
        initializeLeadStatuses();
        initializeLeadSource();
        initializeAdminUser();
        initializeLeadsRecords();initializeUsers();

    }


    private void initializeLeadsRecords() {

//        leadFactoryService.generateLeads(50);
  //offerFactoryService.generateOffers(50);

    }

    private void initializeAdminUser() {
        Optional<User> adminOptional = userRepository.findByUsername("marcinpohl");
        if (adminOptional.isEmpty()) {
            Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            User adminUser = new User();
            adminUser.setUsername("marcinpohl");
            adminUser.setPassword(passwordEncoder.encode("@Zaqwsx6vvso!"));
            adminUser.setRoles(Set.of(adminRole));

            userRepository.save(adminUser);
        }
    }

    private void initializeUsers() {
        List<User> users = Arrays.asList(
                new User(2L, "jagodaszymczak", "$2a$08$AYYWo0zOAgDCTnnfz2Oxa.CfK3QZTw4CUm25i8/tPG/OLS.4/aQIm", "jagoda@w-h.pl", "Jagoda Szymczak", "https://wyposazenie-hotelowe.pl/wp-content/uploads/2023/07/jagoda-500x500.png", 539631014L, true, Set.of()),
                new User(3L, "piotrswitalski", "$2a$08$9P78p0g8.FAnYY79kaQcseitbm3dvqamM4/YM7BZIP.0IewIOPhB.", "piotr@w-h.pl", "Piotr Świtalski", "piotr.png", 506332373L, false, Set.of()),
                new User(5L, "justynachociaj", "$2a$08$Vde2.y4ivqzQ6RpqTVGOdOfROUVVxtKtEWLu47oPdouKGNYWAco/a", "justyna@w-h.pl", "Justyna Chociaj", null, null, false, Set.of()),
                new User(6L, "dorotaprzybyla", "$2a$08$pXFuLvx8HOhFCGQeKyq.YulZPfgKbMI3EOLX4MeJsp5t5br1sLJiu", "dorota@w-h.pl", "Dorota Przybyła", "dorota.png", null, false, Set.of()),
                new User(7L, "dariuszkwiecinski", "$2a$08$AdnZO/Mqulsq6wyPZi7X4uf.Mm84I7FhyvH7dAy3Z30rBCSVEUp9C", "dkwiecinski@w-h.pl", "Dariusz Kwieciński", "https://wyposazenie-hotelowe.pl/wp-content/uploads/2023/07/darek-500x500.png", null, true, Set.of()),
                new User(8L, "grzegorzraczek", "$2a$08$lMg1Lu6IcSxajWoHXLJbUO8CqzPy.SqUoLz6V/TTMFkvNbAtu7U6u", "g.raczek@w-h.pl", "Grzegorz Raczek", "https://wyposazenie-hotelowe.pl/wp-content/uploads/2023/07/grzegorz-1-500x500.png", null, true, Set.of()),
                new User(9L, "krystiankwiecinski", "$2a$08$dYmzJ5ciocH0THpic5uzkejWNfHoyP0dFodJqhmGQ5Uy0rNipwe06", "krystian@w-h.pl", "Krystian Kwieciński", "https://wyposazenie-hotelowe.pl/wp-content/uploads/2023/12/krystian-500x500.jpeg", null, true, Set.of()),
                new User(10L, "joannaapryjas", "$2a$08$/WEXjGx3Y.SyS1DgI/f1deQlJPEJxm7nJ27/hfrMEPjciUOOXSJ8G", "joanna@w-h.pl", "Joanna Apryjas", "https://wyposazenie-hotelowe.pl/wp-content/uploads/2023/07/joanna-1-500x500.png", null, true, Set.of()),

                new User(12L, "rafalhyla", "$2a$08$Gdo0mTrC4rU1fduyggEvpe1zg9I.a09PjB5rv6eQmeK5JP02KOfEK", "info@rafalhyla.pl", "Rafał Hyla", "https://wyposazenie-hotelowe.pl/wp-content/uploads/2023/12/rafal-500x500.jpeg", null, true, Set.of()),

                new User(14L, "kingachalon", "$2a$08$cc6FCEiD7pys88mP9z/ulO5u4vXxyJzaQGNBOB7/KnIX7us1yD1DG", "kinga@w-h.pl", "Kinga Chałon", "kinga (1).png", null, false, Set.of()),
                new User(16L, "agatagryszka", "$2a$08$0J2KcebPmZWVP2pFzSGiPO06vU23Vz3asNsfYXUhNtJiD9T2p3.pC", "agata@bogu.pl", "Agata Gryszka", null, null, false, Set.of()),
                new User(17L, "vasylkozar", "$2a$08$08cB8JGng4Vook5HwnyJmuL6g.SrcweGmqvA5uIegDyhm8i8wGZMC", "vasyl@w-h.pl", "Vasyl Kozar", "34484278_196295397865114_2163958554136936448_n.jpg", null, false, Set.of()),
                new User(18L, "wiktorboiko", "$2a$08$agL3ja/vmFjkBMP8te6Nc.Ah7RVwM9Ov2N1AgNYV.pO0xoU2VhI/i", "wiktor@w-h.pl", "Wiktor Boiko", "z30064582IH,Jason-Statham.jpg", null, false, Set.of()),
                new User(20L, "agnieszkakawa", "$2a$08$AJEySlgFdAjqCbe82ORWLu8cIfJvuJpyttAvhtEiMWJX5lFDMbhvm", "agnieszka.kawa@w-h.pl", "Agnieszka Kawa", null, null, false, Set.of())
        );

        for (User user : users) {
            Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
            if (existingUser.isEmpty()) {
                userRepository.save(user);
            }
        }
    }


    private void initializeLeadStatuses() {
        List<LeadStatus> statuses = Arrays.asList(
                new LeadStatus(1L, "NEW", "Nowy", StatusType.NEUTRAL, false),
                new LeadStatus(2L, "IN_VERIFICATION", "W trakcie weryfikacji", StatusType.NEUTRAL, false),
                new LeadStatus(3L, "IN_QUOTATION", "W trakcie wyceny", StatusType.NEUTRAL, false),
                new LeadStatus(4L, "QUOTATION_SENT", "Oferta wysłana do klienta", StatusType.NEUTRAL, false),
                new LeadStatus(5L, "CLIENT", "Klient", StatusType.POSITIVE, true),
                new LeadStatus(6L, "WAITING_FOR_DOCUMENTS", "Czekamy na uzupełnienie dokumentacji", StatusType.NEUTRAL, false),
                new LeadStatus( 7L, "REJECTED", "Odrzucony", StatusType.NEGATIVE, true),
        new LeadStatus( 8L, "CONTACT_IN_FUTURE", "Kontakt w przyszłosci", StatusType.NEUTRAL, true),
        new LeadStatus( 9L, "ERROR", "Zweryfikuj po migracji danych", StatusType.NEUTRAL, false)

        );

        for(LeadStatus leadStatus : statuses) {
            if(!leadStatusRepository.findByStatusName(leadStatus.getStatusName()).isPresent()){
                leadStatusRepository.save(leadStatus);
            }
        }
    }



    private void initializeLeadSource() {
        List<LeadSource> sources = Arrays.asList(
                new LeadSource(1L, "WEBSITE_FORM", "Formularz ze strony"),
                new LeadSource(2L, "DIRECT_CONTACT", "Kontakt własny"),
                new LeadSource(3L, "RETURNING_CLIENT", "Klient powracający"),
                new LeadSource(4L, "PHONE_CALL", "Rozmowa telefoniczna"),
                new LeadSource(5L, "DIRECT_EMAIL", "Bezpośrednia wiadomość email"),
                new LeadSource(6L, "DESIGN_OFFICE", "Biuro projektowe"),
                new LeadSource(7L, "TRADE_SHOW", "Targi branżowe"),
                new LeadSource(8L, "REFERRAL", "Polecenie"),
                new LeadSource(9L, "SEO", "Klient z wyszukiwarek"),
                new LeadSource(10L, "GOOGLE_ADS", "Google Ads"),
                new LeadSource(11L, "FACEBOOK", "Facebook"),
                new LeadSource(12L, "INSTAGRAM", "Instagram"),
                new LeadSource(13L, "LINKEDIN", "LinkedIn"),
                new LeadSource(14L, "KOMPAS", "Kompas"),
                new LeadSource(15L, "SCOUT", "Zwiadowca"),
                new LeadSource(16L, "OTHER", "Inne"),
                new LeadSource(17L, "TRADE_MAGAZINES", "Magazyny branżowe")
        );

        for (LeadSource leadSource : sources) {
            if (!leadSourceRepository.findByName(leadSource.getName()).isPresent()) {
                leadSourceRepository.save(leadSource);
            }
        }
    }

    public void initializeRoles() {
        for(Role.RoleName roleName : Role.RoleName.values()) {
            if(!roleRepository.findByName(roleName).isPresent()){
                roleRepository.save(new Role(null, roleName));
            }
        }
    }


}
