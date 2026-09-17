package wh.plus.crm.model.aichat;

/**
 * Dostępne czaty AI (jeden model na czat — MVP).
 * <p>{@code agentCode} mapuje na klucz Dify: {@code dify.agent-keys.<agentCode>}.
 * {@code label}/{@code description} są pokazywane w UI (kafle na pulpicie, zakładki).
 * {@code defaultModel} służy tylko do raportu zużycia (mapa CRM ↔ model w Dify).
 */
public enum AiChatApp {

    CLAUDE("asystent-claude", "Claude",
            "Najlepszy do długich tekstów, analiz dokumentów, pisania i redakcji. Świetnie trzyma kontekst i styl.",
            "claude-sonnet-4-6"),
    CHATGPT("asystent-chatgpt", "ChatGPT",
            "Wszechstronny do codziennej pracy: pytania, burza mózgów, kod, szybkie odpowiedzi.",
            "gpt-5.4"),
    GEMINI("asystent-gemini", "Gemini",
            "Mocny w danych, researchu i pracy z obrazami. Dobry do zestawień i analiz multimodalnych.",
            "gemini-3-flash-preview");

    private final String agentCode;
    private final String label;
    private final String description;
    private final String defaultModel;

    AiChatApp(String agentCode, String label, String description, String defaultModel) {
        this.agentCode = agentCode;
        this.label = label;
        this.description = description;
        this.defaultModel = defaultModel;
    }

    public String getAgentCode() { return agentCode; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getDefaultModel() { return defaultModel; }

    /** Bezpieczne parsowanie kodu z requestu (case-insensitive). Zwraca null dla nieznanego. */
    public static AiChatApp fromCode(String code) {
        if (code == null) return null;
        for (AiChatApp a : values()) {
            if (a.name().equalsIgnoreCase(code)) return a;
        }
        return null;
    }
}
