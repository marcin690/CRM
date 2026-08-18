package wh.plus.crm.dto.aivaluation;

import java.time.LocalDateTime;

public record ValuationMessageDto(Long id, String role, String content, LocalDateTime createdAt) {}
