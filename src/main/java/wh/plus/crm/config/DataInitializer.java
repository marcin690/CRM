package wh.plus.crm.config;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import wh.plus.crm.model.Role;
import wh.plus.crm.repository.RoleRepository;

@Component
@AllArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;

    @PostMapping
    public void initialize() {
        for(Role.RoleName roleName : Role.RoleName.values()) {
            if(!roleRepository.findByName(roleName).isPresent()){
                roleRepository.save(new Role(null, roleName));
            }
        }
    }
}
