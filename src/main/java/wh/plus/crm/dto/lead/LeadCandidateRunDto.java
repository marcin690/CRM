package wh.plus.crm.dto.lead;

import java.time.LocalDateTime;

/** Status przebiegu agenta generującego kandydatów (do pollingu na froncie). */
public record LeadCandidateRunDto(
        Long id,
        String status,
        String tryb,
        Integer dniNewsy,
        Integer dniPrzetargi,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Integer found,
        String message,
        String startedBy
) {}
