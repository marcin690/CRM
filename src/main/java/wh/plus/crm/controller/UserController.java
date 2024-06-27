package wh.plus.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.UserDTO;
import wh.plus.crm.model.User;
import wh.plus.crm.repository.UserRepository;
import wh.plus.crm.service.UserService;

import java.util.List;

@RestController()
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;


    @GetMapping
    public ResponseEntity<List<UserDTO>> findAll() {
        List<UserDTO> users = userService.findAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }


    @DeleteMapping
    public ResponseEntity<User> deleteUsers(@RequestBody List<Long> ids) {
        userService.deleteUsers(ids);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{id}")
    public ResponseEntity<UserDTO> editUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {

        userDTO.setId(id);
        UserDTO updateUser = userService.updateUserPartially(userDTO);
        return new ResponseEntity<>(updateUser, HttpStatus.OK);
    }

    @GetMapping("/sellers")
    public List<User> getSellers() {
        return userRepository.findByIsSalesRepresentativeTrue();
    }
}
