package wh.plus.crm.config;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import wh.plus.crm.model.Role;
import wh.plus.crm.model.lead.LeadStatus;
import wh.plus.crm.repository.LeadStatusRepository;
import wh.plus.crm.repository.RoleRepository;
import wh.plus.crm.model.lead.LeadStatus.StatusType;


import java.util.Arrays;
import java.util.List;

@Component
@AllArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final LeadStatusRepository leadStatusRepository;

    @PostConstruct
    @Transactional
    public void initialize() {
        initializeRoles();
        initializeLeadStatuses();
    }

    private void initializeLeadStatuses() {
        List<LeadStatus> statuses = Arrays.asList(
                new LeadStatus(1L, "NEW", "Nowy", StatusType.NEUTRAL, false),
                new LeadStatus(2L, "IN_VERIFICATION", "W trakcie weryfikacji", StatusType.NEUTRAL, false),
                new LeadStatus(3L, "IN_QUOTATION", "W trakcie wyceny", StatusType.NEUTRAL, false),
                new LeadStatus(4L, "QUOTATION_SENT", "Oferta wysłana do klienta", StatusType.NEUTRAL, false),
                new LeadStatus(5L, "CLIENT", "Klient", StatusType.POSITIVE, false),
                new LeadStatus(6L, "WAITING_FOR_DOCUMENTS", "Czekamy na uzupełnienie dokumentacji", StatusType.NEUTRAL, false),
                new LeadStatus( 7L, "REJECTED", "Odrzucony", StatusType.NEGATIVE, true)
        );

        for(LeadStatus leadStatus : statuses) {
            if(!leadStatusRepository.findByStatusName(leadStatus.getStatusName()).isPresent()){
                leadStatusRepository.save(leadStatus);
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
