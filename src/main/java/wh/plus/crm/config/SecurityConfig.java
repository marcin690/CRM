package wh.plus.crm.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import wh.plus.crm.security.JwtAuthenticationFilter;
import wh.plus.crm.security.JwtUtil;
import wh.plus.crm.service.UserService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userService);
    }

    private static final String[] AUTH_WHITELIST = {
            "/auth/register",
            "/auth/login",
            "/api/form-submissions",   // publiczny endpoint dla landing-page'y (PublicLeadController)
            "/api/form-submissions/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable().cors().and()
                .authorizeHttpRequests(authorize ->
                        authorize
                                // SSE (czat AI) po zakończeniu robi async-dispatch z powrotem przez łańcuch
                                // security z anonimowym kontekstem — bez tego leci AccessDenied. Początkowe
                                // żądanie (DispatcherType.REQUEST) nadal wymaga uwierzytelnienia.
                                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers(AUTH_WHITELIST).permitAll()
                                .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            Object jwtError = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);
                            String message;
                            if ("expired".equals(jwtError)) {
                                message = "Sesja wygasła. Zaloguj się ponownie.";
                            } else if ("userNotFound".equals(jwtError)) {
                                message = "Użytkownik z tokenu nie istnieje.";
                            } else if ("invalid".equals(jwtError)) {
                                message = "Token jest nieprawidłowy.";
                            } else {
                                message = "Brak autoryzacji — zaloguj się.";
                            }
                            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized", message);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            writeJson(response, HttpServletResponse.SC_FORBIDDEN, "forbidden",
                                    "Brak uprawnień do tej operacji.");
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /** Spójna odpowiedź JSON dla błędów auth (401/403). Komunikaty są stałe, bez znaków wymagających escapowania. */
    private static void writeJson(HttpServletResponse response, int status, String error, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
