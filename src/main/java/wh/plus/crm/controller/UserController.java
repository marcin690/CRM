package wh.plus.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.UserDTO;
import wh.plus.crm.model.Role;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.UserRepository;
import wh.plus.crm.service.UserService;

import java.util.Arrays;
import java.util.List;

@RestController()
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;


    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserDTO>> findAll() {
        List<UserDTO> users = userService.findAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }


    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> create(@RequestBody UserDTO dto) {
        return new ResponseEntity<>(userService.createUser(dto), HttpStatus.CREATED);
    }


    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> deleteUsers(@RequestBody List<Long> ids) {
        userService.deleteUsers(ids);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> editUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {

        userDTO.setId(id);
        UserDTO updateUser = userService.updateUserPartially(userDTO);
        return new ResponseEntity<>(updateUser, HttpStatus.OK);
    }

    /** Admin: ustawienie ról, zespołu, uprawnień modułowych i flagi sprzedawcy. */
    @PatchMapping("/{id}/access")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> updateAccess(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateAccess(id, userDTO));
    }

    /** Lista dostępnych ról (do selecta w zakładce Users). */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<String> roles() {
        return Arrays.stream(Role.RoleName.values()).map(Enum::name).toList();
    }

    @GetMapping("/sellers")
    public List<User> getSellers() {
        return userRepository.findByIsSalesRepresentativeTrue();
    }
}
