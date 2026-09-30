package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import wh.plus.crm.dto.UserDTO;
import wh.plus.crm.mapper.UserMapper;
import wh.plus.crm.model.Role;
import wh.plus.crm.model.user.SalesTeam;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.RoleRepository;
import wh.plus.crm.repository.SalesTeamRepository;
import wh.plus.crm.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final SalesTeamRepository salesTeamRepository;

    /** DTO wzbogacone o role, zespół i uprawnienia (do zakładki Users). */
    private UserDTO toDto(User u) {
        UserDTO dto = userMapper.userToUserDTO(u);
        dto.setRoles(u.getRoles() == null ? new HashSet<>()
                : u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()));
        dto.setTeamId(u.getTeam() != null ? u.getTeam().getId() : null);
        dto.setTeamName(u.getTeam() != null ? u.getTeam().getName() : null);
        dto.setIsSalesRepresentative(u.isSalesRepresentative());
        dto.setPermissions(u.getPermissions() == null ? new HashSet<>() : new HashSet<>(u.getPermissions()));
        dto.setBlocked(Boolean.TRUE.equals(u.getBlocked()));
        return dto;
    }

    /** Admin: utworzenie użytkownika z rolą, zespołem i uprawnieniami (bezpieczne — DTO, nie encja z requestu). */
    public UserDTO createUser(UserDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nazwa użytkownika jest wymagana");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hasło jest wymagane");
        }
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Użytkownik o takiej nazwie już istnieje");
        }
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(passwordEncoder.encode(dto.getPassword()));
        u.setFullname(dto.getFullname());
        u.setEmail(dto.getEmail());
        u.setPhone(dto.getPhone());

        Set<Role> roles = parseRoles(dto.getRoles());
        if (roles.isEmpty()) {
            roles.add(roleRepository.findByName(Role.RoleName.USER)
                    .orElseGet(() -> roleRepository.save(new Role(null, Role.RoleName.USER))));
        }
        u.setRoles(roles);

        if (dto.getTeamId() != null && dto.getTeamId() > 0) {
            u.setTeam(salesTeamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales Team not found")));
        }
        u.setPermissions(dto.getPermissions() != null ? new HashSet<>(dto.getPermissions()) : new HashSet<>());
        if (dto.getIsSalesRepresentative() != null) {
            u.setSalesRepresentative(dto.getIsSalesRepresentative());
        }
        userRepository.save(u);
        return toDto(u);
    }

    /** Mapuje nazwy ról na encje; nieznana nazwa → 400 zamiast 500. */
    private Set<Role> parseRoles(Set<String> names) {
        Set<Role> roles = new HashSet<>();
        if (names == null) return roles;
        for (String name : names) {
            Role.RoleName rn;
            try {
                rn = Role.RoleName.valueOf(name);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznana rola: " + name);
            }
            roles.add(roleRepository.findByName(rn)
                    .orElseGet(() -> roleRepository.save(new Role(null, rn))));
        }
        return roles;
    }

    /** Admin: aktualizacja ról, zespołu, uprawnień modułowych i flagi sprzedawcy. */
    public UserDTO updateAccess(Long id, UserDTO dto) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (dto.getRoles() != null) {
            u.setRoles(parseRoles(dto.getRoles()));
        }

        if (dto.getTeamId() != null) {
            if (dto.getTeamId() <= 0) {
                u.setTeam(null); // 0/-1 = odpięcie od zespołu
            } else {
                SalesTeam team = salesTeamRepository.findById(dto.getTeamId())
                        .orElseThrow(() -> new IllegalArgumentException("Sales Team not found"));
                u.setTeam(team);
            }
        }

        if (dto.getPermissions() != null) {
            u.setPermissions(new HashSet<>(dto.getPermissions()));
        }
        if (dto.getIsSalesRepresentative() != null) {
            u.setSalesRepresentative(dto.getIsSalesRepresentative());
        }
        if (dto.getBlocked() != null) {
            u.setBlocked(dto.getBlocked());
        }

        userRepository.save(u);
        return toDto(u);
    }


    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(User user) {

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Set<Role> roles = new HashSet<>();
        for (Role role : user.getRoles()) {
            Role savedRole = roleRepository.findByName(role.getName())
                    .orElseGet(() -> roleRepository.save(new Role(null, role.getName())));
            roles.add(savedRole);
        }
        user.setRoles(roles);
        return userRepository.save(user);
    }


    public List<UserDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    public void resetPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public void deleteUsers(List<Long> ids) {
        userRepository.deleteAllByIdIn(ids);
    }

    public UserDTO updateUserPartially(UserDTO userDTO) {
        Optional<User> existingUserOptional = userRepository.findById(userDTO.getId());
        if(existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();
           if(userDTO.getFullname() != null) {
               existingUser.setFullname(userDTO.getFullname());
            }
           if(userDTO.getEmail() != null) {
               existingUser.setEmail(userDTO.getEmail());
           }
          if(userDTO.getPhone() != null) {
              existingUser.setPhone(userDTO.getPhone());
          }
          userRepository.save(existingUser);
          return userMapper.userToUserDTO(existingUser);
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }


}
