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

    /** Dozwolone poziomy (etykiety zgodne z tym, co przyjmuje zmienna 'poziom' w Dify). */
    public static final java.util.List<String> POZIOMY = java.util.List.of("Szybki", "Standard", "Zaawansowany");

    /** Normalizuje poziom z requestu do jednej z dozwolonych etykiet (domyślnie Standard). */
    public static String normPoziom(String p) {
        if (p != null) {
            for (String v : POZIOMY) if (v.equalsIgnoreCase(p.trim())) return v;
        }
        return "Standard";
    }

    /** Mapa poziom → konkretny model (do raportu zużycia; musi odpowiadać routingowi w Dify). */
    public String modelFor(String poziom) {
        String p = normPoziom(poziom);
        return switch (this) {
            case CLAUDE -> switch (p) {
                case "Szybki" -> "claude-haiku-4-5";
                case "Zaawansowany" -> "claude-opus-4-6";
                default -> "claude-sonnet-4-6";
            };
            case CHATGPT -> switch (p) {
                case "Szybki" -> "gpt-5.4-nano";
                case "Zaawansowany" -> "gpt-5.4";
                default -> "gpt-5.4-mini";
            };
            case GEMINI -> switch (p) {
                case "Szybki" -> "gemini-2.5-flash-lite";
                case "Zaawansowany" -> "gemini-3.1-pro-preview";
                default -> "gemini-3-flash-preview";
            };
        };
    }
}
