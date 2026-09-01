package wh.plus.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-konfiguracja poczty NIE jest wykluczana — jest warunkowa od spring.mail.host.
 * Bez SMTP JavaMailSender/EmailService się nie tworzy (start bez zmian); z SMTP (env Railway) poczta działa.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }

}
