package wh.plus.crm.dto.aivaluation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO zadania wyceny. W widoku listy {@code messages} jest puste (null),
 * w widoku szczegółu zawiera pełny wątek czatu.
 */
public record ValuationJobDto(
        Long id,
        String agentCode,
        String agentName,
        String title,
        String note,
        String status,
        String inputFileUrl,
        String excelUrl,
        String resultMarkdown,
        String errorMessage,
        String conversationId,
        Double costUsd,
        String createdBy,
        LocalDateTime createdAt,
        String comparisonResult,
        List<ValuationMessageDto> messages
) {}
