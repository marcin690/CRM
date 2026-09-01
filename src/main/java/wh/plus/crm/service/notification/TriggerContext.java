package wh.plus.crm.service.notification;

import wh.plus.crm.model.EntityType;

import java.util.Map;

/**
 * Dane pojedynczego zdarzenia przekazywane do silnika triggerów.
 *
 * @param inAppContent   krótka treść powiadomienia w aplikacji (dzwonek)
 * @param emailSubject   temat e-maila
 * @param emailTemplate  nazwa szablonu Thymeleaf (plik templates/{name}.html)
 * @param emailVariables zmienne do szablonu e-mail
 * @param entityType     typ powiązanej encji (dla linku/kontekstu)
 * @param entityId       ID powiązanej encji
 */
public record TriggerContext(
        String inAppContent,
        String emailSubject,
        String emailTemplate,
        Map<String, Object> emailVariables,
        EntityType entityType,
        Long entityId
) {}
