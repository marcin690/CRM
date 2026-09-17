package wh.plus.crm.dto.aichat;

import java.math.BigDecimal;

/** Projekcja wiersza raportu zużycia AI per użytkownik (agregat, bez treści rozmów). */
public interface UserUsageRow {
    Long getUserId();
    String getUserEmail();
    long getMessages();
    long getConversations();
    long getPromptTokens();
    long getCompletionTokens();
    BigDecimal getCostUsd();
}
