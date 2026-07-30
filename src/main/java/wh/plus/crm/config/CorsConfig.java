package wh.plus.crm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lista dozwolonych origin-ów dla CORS.
 *
 * Konfigurowana przez właściwość {@code app.cors.allowed-origins} (CSV, env
 * {@code CORS_ALLOWED_ORIGINS} w Railway) ORAZ zawsze uzupełniana o wbudowaną
 * listę własnych domen WH z wildcardami subdomen ({@link #ALWAYS_ALLOWED}).
 *
 * Dzięki temu warianty {@code www.} i dowolne subdomeny (np. www.w-h.pl)
 * są dozwolone niezależnie od tego, co ustawiono w env — to naprawia klasę
 * błędów "user na www.* dostaje CORS", bez ryzyka pominięcia wariantu w liście.
 */
@Configuration
@Slf4j
public class CorsConfig {

    /**
     * Zawsze dozwolone wzorce (własne domeny WH). Wildcard {@code *.} pokrywa
     * www.* i inne subdomeny; apex (bez subdomeny) musi być osobno.
     * allowedOriginPatterns wspiera wildcardy i działa z allowCredentials(true).
     */
    private static final String[] ALWAYS_ALLOWED = {
            "https://w-h.pl", "https://*.w-h.pl",
            "https://wyposazenie-hotelowe.pl", "https://*.wyposazenie-hotelowe.pl",
            "https://whplus.com.pl", "https://*.whplus.com.pl",
            "https://*.azurestaticapps.net"
    };

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        // Kolejność zachowana, duplikaty usunięte: env/CSV + zawsze dozwolone WH.
        Set<String> merged = new LinkedHashSet<>();
        Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(merged::add);
        merged.addAll(Arrays.asList(ALWAYS_ALLOWED));
        final String[] origins = merged.toArray(new String[0]);

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
