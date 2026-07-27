package wh.plus.crm.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import wh.plus.crm.service.UserService;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** Atrybut requestu z powodem odrzucenia tokenu — odczytywany przez authenticationEntryPoint. */
    public static final String JWT_ERROR_ATTRIBUTE = "jwtError";

    private final JwtUtil jwtUtil;

    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwtToken);
            } catch (ExpiredJwtException e) {
                // Wygasły token nie może już rzucać nieobsłużonym wyjątkiem (dawniej -> 500).
                log.warn("Odrzucono token JWT: wygasł (sub='{}', {} {})",
                        e.getClaims().getSubject(), request.getMethod(), request.getRequestURI());
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "expired");
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("Odrzucono token JWT: nieprawidłowy ({} {}): {}",
                        request.getMethod(), request.getRequestURI(), e.getMessage());
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "invalid");
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userService.loadUserByUsername(username);
                if (jwtUtil.validateToken(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                } else {
                    log.warn("Token JWT nie przeszedł walidacji dla użytkownika '{}' ({} {})",
                            username, request.getMethod(), request.getRequestURI());
                    request.setAttribute(JWT_ERROR_ATTRIBUTE, "invalid");
                }
            } catch (UsernameNotFoundException e) {
                log.warn("Użytkownik z tokenu JWT nie istnieje: '{}' ({} {})",
                        username, request.getMethod(), request.getRequestURI());
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "userNotFound");
            }
        }
        filterChain.doFilter(request, response);
    }
}
