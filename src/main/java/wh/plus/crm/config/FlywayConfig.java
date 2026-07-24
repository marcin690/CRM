package wh.plus.crm.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Strategia startu Flyway: najpierw {@code repair()}, potem {@code migrate()}.
 *
 * Powód: wcześniejsza wersja V12 wywaliła się w połowie (Flyway leci PRZED Hibernate
 * ddl-auto, więc tabela `invoice` jeszcze nie istniała) i zostawiła NIEUDANY wpis w
 * `flyway_schema_history`. Bez repair każdy kolejny start konczy sie
 * FlywayValidateException i aplikacja nie wstaje.
 *
 * {@code repair()} usuwa nieudane wpisy i wyrównuje sumy kontrolne, po czym
 * {@code migrate()} stosuje poprawioną (defensywną) V12 na czysto.
 *
 * Po jednym udanym deployu ten bean można usunąć — repair-on-boot maskuje ewentualne
 * przyszłe nieudane migracje, więc nie zostawiamy go na stałe bez potrzeby.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
