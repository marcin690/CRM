package wh.plus.crm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.model.user.User;
import wh.plus.crm.model.auth.AuthenticationRequest;
import wh.plus.crm.model.auth.AuthenticationResponse;
import wh.plus.crm.security.JwtUtil;
import wh.plus.crm.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    public static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password cannot be null or empty");
        }
        try {
            userService.saveUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during registration: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> resetPasswordRequest) {
        try {
            String username = resetPasswordRequest.get("username");
            String newPassword = resetPasswordRequest.get("newPassword");
            userService.resetPassword(username, newPassword);
            return ResponseEntity.ok("Hasło zostało zmienione");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred during password reset.");
        }
    }

    /**
     * Zwraca username i listę authorities zalogowanego usera.
     * Frontend używa do gating-u UI (np. zakładka „Fakturownia" tylko dla ADMIN).
     * Wymagany Bearer token — endpoint pod auth.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }
        Object principal = auth.getPrincipal();
        String username = (principal instanceof User u) ? u.getUsername() : auth.getName();
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return ResponseEntity.ok(Map.of(
                "username", username,
                "authorities", authorities
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest authenticationRequest) {
        logger.info("Login request: {}", authenticationRequest);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
            );
            User user = (User) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(user);
            return ResponseEntity.ok(new AuthenticationResponse(jwt));
        } catch (AuthenticationException e) {
            logger.error("Authentication failed: {}", authenticationRequest);
            return ResponseEntity.status(401).body("Authentication failed.");
        }
    }

}
