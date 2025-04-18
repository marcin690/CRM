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
        initializeLeadsRecords();


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
