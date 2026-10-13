package wh.plus.crm.dto.lead;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.LocalDateTime;

/**
 * Kandydat na leada zwracany do frontu. {@code sygnaly} i {@code szczegoly}
 * są wstrzykiwane jako surowy JSON ({@link JsonRawValue}), więc front dostaje
 * gotową tablicę/obiekt bez podwójnego parsowania.
 */
public record LeadCandidateDto(
        Long id,
        String nazwa,
        String miasto,
        String wojewodztwo,
        String rodzaj,
        int punkty,
        boolean przetarg,
        String status,
        @JsonRawValue String sygnaly,
        @JsonRawValue String szczegoly,
        String zrodloUrl,
        LocalDateTime generatedAt,
        Long acceptedLeadId
) {}
