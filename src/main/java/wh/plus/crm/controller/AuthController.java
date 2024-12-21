package wh.plus.crm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.model.user.User;
import wh.plus.crm.model.auth.AuthenticationRequest;
import wh.plus.crm.model.auth.AuthenticationResponse;
import wh.plus.crm.security.JwtUtil;
import wh.plus.crm.service.UserService;

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
        logger.info("Register request for user: {}", user);
        try {
            if (userService.findByUsername(user.getUsername()).isPresent()) {
                logger.warn("Username is already taken: {}", user.getUsername());
                return ResponseEntity.badRequest().body("Username is already taken.");
            }
            userService.saveUser(user);
            logger.info("User registered successfully: {}", user);
            return ResponseEntity.ok("User registered successfully.");
        } catch (Exception e) {
            logger.error("Error during registration: ", e);
            return ResponseEntity.status(500).body("An error occurred during registration.");
        }
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
