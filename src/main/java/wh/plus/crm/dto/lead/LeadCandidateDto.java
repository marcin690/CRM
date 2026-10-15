package wh.plus.crm.dto.lead;

import com.fasterxml.jackson.annotation.JsonRawValue;
import wh.plus.crm.model.lead.LeadCandidate;

import java.time.LocalDateTime;

/**
 * Kandydat na leada zwracany do frontu. {@code sygnaly} i {@code szczegoly}
 * są wstrzykiwane jako surowy JSON ({@link JsonRawValue}), więc front dostaje
 * gotową tablicę/obiekt bez podwójnego parsowania.
 *
 * <p>{@code decidedBy}/{@code decidedAt}/{@code rejectionNote} wypełnione dla
 * kandydatów ACCEPTED/REJECTED — zasilają widok historii decyzji (kto/kiedy).
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
        Long acceptedLeadId,
        String decidedBy,
        LocalDateTime decidedAt,
        String rejectionNote
) {
    public static LeadCandidateDto from(LeadCandidate c) {
        return new LeadCandidateDto(
                c.getId(), c.getNazwa(), c.getMiasto(), c.getWojewodztwo(), c.getRodzaj(),
                c.getPunkty(), c.isPrzetarg(), c.getStatus() == null ? null : c.getStatus().name(),
                c.getSygnalyJson() == null ? "[]" : c.getSygnalyJson(),
                c.getSzczegolyJson() == null ? "{}" : c.getSzczegolyJson(),
                c.getZrodloUrl(), c.getGeneratedAt(), c.getAcceptedLeadId(),
                c.getDecidedBy(), c.getDecidedAt(), c.getRejectionNote());
    }
}
