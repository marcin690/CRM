package wh.plus.crm.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import wh.plus.crm.service.fakturownia.FakturowniaReadOnlyInterceptor;

import java.time.Duration;

@Configuration
@EnableScheduling
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Dedykowany RestTemplate dla Fakturowni z interceptorem
     * {@link FakturowniaReadOnlyInterceptor} blokującym każdy non-GET request.
     * To architekturalna gwarancja read-only — niezależna od tego, co napisze
     * przyszły kod w {@code FakturowniaClient} lub innym serwisie.
     */
    @Bean("fakturowniaRestTemplate")
    public RestTemplate fakturowniaRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .additionalInterceptors(new FakturowniaReadOnlyInterceptor())
                .build();
    }
}
