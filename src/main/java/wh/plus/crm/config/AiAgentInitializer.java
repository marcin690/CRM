package wh.plus.crm.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wh.plus.crm.model.aivaluation.AiAgent;
import wh.plus.crm.repository.AiAgentRepository;

/**
 * Seed rejestru agentów AI. Idempotentny — dodaje agenta tylko gdy brak wpisu o danym {@code code}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiAgentInitializer {

    private final AiAgentRepository agentRepository;

    @PostConstruct
    public void init() {
        seed("meble-wspolne", "Wycena mebli — części wspólne",
                "Lady recepcyjne, bary, zabudowy live cooking, wydawki, zagłówki, panele ścienne, "
                        + "obudowy słupów i inne elementy niestandardowe. Wycena materiałowa hurtowa.");
        seed("meble-skrzyniowe", "Wycena mebli skrzyniowych",
                "Szafy, komody, biurka, kontenerki, zabudowy skrzyniowe. Wycena materiałowa hurtowa.");
    }

    private void seed(String code, String name, String description) {
        if (agentRepository.findByCode(code).isPresent()) return;
        AiAgent a = new AiAgent();
        a.setCode(code);
        a.setName(name);
        a.setDescription(description);
        a.setActive(true);
        agentRepository.save(a);
        log.info("Zainicjowano agenta AI: {}", code);
    }
}
