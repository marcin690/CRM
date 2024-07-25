package wh.plus.crm.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.UserDTO;
import wh.plus.crm.mapper.UserMapper;
import wh.plus.crm.model.Role;
import wh.plus.crm.model.User;
import wh.plus.crm.repository.LeadRepository;
import wh.plus.crm.repository.RoleRepository;
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


    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(User user) {
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
                .map(userMapper::userToUserDTO)
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
