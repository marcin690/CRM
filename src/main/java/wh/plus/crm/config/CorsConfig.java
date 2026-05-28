package wh.plus.crm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Lista dozwolonych origin-ów dla CORS.
 *
 * Konfigurowana przez właściwość {@code app.cors.allowed-origins} (CSV).
 * W produkcji ustawiona zmienną środowiskową {@code CORS_ALLOWED_ORIGINS}
 * (np. w Railway: Settings → Variables → CORS_ALLOWED_ORIGINS=...).
 *
 * Default (gdy zmiennej nie ma) — bezpieczna lista znanych domen
 * + http://localhost:3000 dla lokalnego dev frontendu.
 */
@Configuration
@Slf4j
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        final String[] origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        log.info("CORS allowed origin patterns ({}): {}", origins.length, String.join(", ", origins));

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // allowedOriginPatterns (NIE allowedOrigins) — pozwala na wildcardy portów
                // np. "http://localhost:*" matchuje :80, :3000, :8000 itd.
                registry.addMapping("/**")
                        .allowedOriginPatterns(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                        .allowedHeaders("*")
                        .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials")
                        .allowCredentials(true);
            }
        };
    }
}
