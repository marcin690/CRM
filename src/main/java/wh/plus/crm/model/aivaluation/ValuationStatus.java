package wh.plus.crm.model.aivaluation;

/**
 * Status zadania wyceny AI.
 * QUEUED — utworzone, czeka w kolejce.
 * PROCESSING — trwa wywołanie agenta Dify.
 * PRICED — wycena gotowa (agent zwrócił podsumowanie + link do Excela).
 * ACTION_REQUIRED — agent zadał pytania doprecyzowujące, wymaga reakcji użytkownika.
 * FAILED — błąd wywołania.
 */
public enum ValuationStatus {
    QUEUED,
    PROCESSING,
    PRICED,
    ACTION_REQUIRED,
    FAILED
}
