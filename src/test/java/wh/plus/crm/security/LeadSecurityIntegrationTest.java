package wh.plus.crm.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import wh.plus.crm.config.SecurityConfig;
import wh.plus.crm.controller.LeadController;
import wh.plus.crm.dto.lead.LeadDTO;
import wh.plus.crm.mapper.ClientMapper;
import wh.plus.crm.model.Role;
import wh.plus.crm.model.user.User;
import wh.plus.crm.service.LeadService;
import wh.plus.crm.service.UserService;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Weryfikuje realny łańcuch bezpieczeństwa (SecurityConfig + JwtAuthenticationFilter + @PreAuthorize)
 * dla POST /leads. Sedno hotfixa: @PreAuthorize używa teraz hasAuthority(...) zamiast hasRole(...),
 * bo role w bazie nie mają prefiksu ROLE_ (Role.getAuthority() zwraca np. "ADMIN").
 */
@WebMvcTest(LeadController.class)
@Import({SecurityConfig.class, JwtUtil.class})
class LeadSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UserService userService;

    @MockBean
    private LeadService leadService;

    @MockBean
    private ClientMapper clientMapper;

    private User userWithAuthorities(String username, Role.RoleName... roleNames) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("irrelevant");
        Role[] roles = new Role[roleNames.length];
        for (int i = 0; i < roleNames.length; i++) {
            roles[i] = new Role((long) (i + 1), roleNames[i]);
        }
        user.setRoles(Set.of(roles));
        return user;
    }

    /** GŁÓWNY DOWÓD: użytkownik z authority "ADMIN" (bez prefiksu ROLE_) MOŻE utworzyć leada. */
    @Test
    void adminCanCreateLead() throws Exception {
        User admin = userWithAuthorities("marcinpohl", Role.RoleName.ADMIN);
        when(userService.loadUserByUsername("marcinpohl")).thenReturn(admin);
        when(leadService.save(any(LeadDTO.class))).thenReturn(new LeadDTO());
        String token = jwtUtil.generateToken(admin);

        mockMvc.perform(post("/leads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    /** Użytkownik z rolą USER też przechodzi (hasAuthority('USER')). */
    @Test
    void userRoleCanCreateLead() throws Exception {
        User user = userWithAuthorities("handlowiec", Role.RoleName.USER);
        when(userService.loadUserByUsername("handlowiec")).thenReturn(user);
        when(leadService.save(any(LeadDTO.class))).thenReturn(new LeadDTO());
        String token = jwtUtil.generateToken(user);

        mockMvc.perform(post("/leads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    /** Zalogowany user bez wymaganej roli -> 403 (a NIE mylące 401). Rozstrzyga wątpliwość 401 vs 403. */
    @Test
    void authenticatedButWrongRoleGets403() throws Exception {
        User user = userWithAuthorities("ktos", Role.RoleName.ADMINISTRATION);
        when(userService.loadUserByUsername("ktos")).thenReturn(user);
        String token = jwtUtil.generateToken(user);

        mockMvc.perform(post("/leads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    /** Brak tokenu -> 401 z czytelnym komunikatem. */
    @Test
    void noTokenGets401() throws Exception {
        mockMvc.perform(post("/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    /** Nieprawidłowy token -> 401 (a NIE 500) z komunikatem "invalid". Dowód hardeningu filtra. */
    @Test
    void malformedTokenGets401NotServerError() throws Exception {
        mockMvc.perform(post("/leads")
                        .header("Authorization", "Bearer to.nie.jest.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token jest nieprawidłowy."));
    }
}
