package wh.plus.crm.model.notification;

/**
 * Typy zdarzeń, które mogą wyzwalać powiadomienie (silnik triggerów).
 *
 * Dodanie kolejnego triggera = dopisanie wartości tutaj + wywołanie
 * {@code notificationTriggerService.fire(TYP, kontekst)} w odpowiednim miejscu.
 * Konfiguracja (kto, jakie kanały, czy włączony) żyje w encji
 * {@link NotificationTrigger} i jest edytowalna z UI — bez zmian w kodzie.
 */
public enum NotificationTriggerType {

    /** Nowy lead dodany automatycznie z formularza na stronie (PublicLeadController). */
    NEW_LEAD

    // Przyszłe (moduły dostawców, zadań itd.):
    // NEW_SUPPLIER,
    // TASK_ASSIGNED,
    // ...
}
