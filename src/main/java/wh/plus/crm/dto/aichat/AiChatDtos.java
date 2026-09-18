package wh.plus.crm.dto.aichat;

import java.time.LocalDateTime;
import java.util.List;

/** Zestaw DTO modułu Asystent AI. Front nigdy nie widzi dify_conversation_id ani kluczy. */
public final class AiChatDtos {

    private AiChatDtos() {}

    /** Opis czatu do kafli/zakładek — z Dify /parameters (z fallbackiem na stały opis). */
    public record AiAppDto(String code, String label, String description,
                           String openingStatement, List<String> suggestedQuestions) {}

    /** Pozycja listy rozmów (lokalne id, oznaczenie czatu, poziom modelu). */
    public record AiConversationDto(Long id, String app, String appLabel, String title,
                                    String poziom, LocalDateTime updatedAt) {}

    /** Para pytanie/odpowiedź z historii (treść z Dify). */
    public record AiMessageDto(String id, String query, String answer, LocalDateTime createdAt) {}

    /** Strona historii (stronicowanie w górę). */
    public record AiMessagesPage(List<AiMessageDto> messages, boolean hasMore) {}

    /** Body POST /api/ai/chat. conversationId puste = nowa rozmowa. poziom tylko przy nowej. */
    public record ChatRequest(String app, Long conversationId, String poziom, String query, List<String> fileIds) {}

    public record RenameRequest(String title) {}

    public record FeedbackRequest(String rating, String content) {}

    /** Wynik uploadu obrazu — front dostaje tylko id pliku (z Dify). */
    public record FileUploadedDto(String id) {}

    /** Wiersz raportu zużycia dla admina (agregat). */
    public record UsageReportRow(Long userId, String userEmail, long conversations, long messages,
                                 long promptTokens, long completionTokens, double costUsd) {}
}
