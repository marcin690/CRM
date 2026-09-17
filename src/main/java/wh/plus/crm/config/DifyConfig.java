package wh.plus.crm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Konfiguracja integracji z Dify (agenci AI).
 *
 * <p>base-url oraz klucze API pochodzą z konfiguracji (env). Klucz jest wiązany
 * z agentem po jego {@code code}, np. {@code dify.agent-keys.meble-wspolne=...}.
 */
@Configuration
@EnableAsync
public class DifyConfig {

    /** Właściwości bindowane z prefiksu {@code dify.*}. */
    @Component
    @ConfigurationProperties(prefix = "dify")
    @Getter
    @Setter
    public static class DifyProperties {
        /** Bazowy adres API Dify, np. https://api.agent.w-h.pl/v1 (prod: sieć prywatna Railway). */
        private String baseUrl;
        /** Klucze API per kod agenta: dify.agent-keys.<code>=app-xxx */
        private Map<String, String> agentKeys = new HashMap<>();
        /**
         * Prefiks identyfikatora użytkownika przekazywanego do Dify: {@code <prefix>:<userId>}.
         * Oddziela środowiska (prod/staging) korzystające z tych samych aplikacji Dify.
         */
        private String userPrefix = "wh-crm-prod";
    }

    /**
     * Dedykowany RestTemplate dla Dify z DŁUGIM read-timeout — pełna wycena
     * (analiza + 7× RAG + generacja Excela) potrafi trwać kilkadziesiąt sekund.
     */
    @Bean("difyRestTemplate")
    public RestTemplate difyRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(360)) // pełna wycena z RAG+Excel trwa ~250s (zmierzone), zapas na zmienność
                .build();
    }

    /** Pula wątków kolejki wycen — ogranicza współbieżność wywołań do Dify. */
    @Bean("valuationExecutor")
    public Executor valuationExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(3);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("valuation-");
        ex.initialize();
        return ex;
    }

    /** Pula wątków dla streamingu czatu AI (jeden wątek pompuje SSE Dify → przeglądarka). */
    @Bean("aiChatExecutor")
    public Executor aiChatExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("ai-chat-");
        ex.initialize();
        return ex;
    }
}
