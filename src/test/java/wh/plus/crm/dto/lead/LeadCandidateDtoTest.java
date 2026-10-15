package wh.plus.crm.dto.lead;

import org.junit.jupiter.api.Test;
import wh.plus.crm.model.lead.LeadCandidate;
import wh.plus.crm.model.lead.LeadCandidateStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO kandydata musi wystawiać „kto/kiedy zdecydował" (decidedBy/decidedAt)
 * oraz powód odrzucenia — do widoku list Dodane/Odrzucone.
 */
class LeadCandidateDtoTest {

    @Test
    void zaakceptowanyNiesieKtoKiedyIdLeada() {
        LeadCandidate c = new LeadCandidate();
        c.setId(5L);
        c.setNazwa("Hotel X");
        c.setStatus(LeadCandidateStatus.ACCEPTED);
        LocalDateTime t = LocalDateTime.of(2026, 10, 15, 9, 0);
        c.setDecidedBy("jan.kowalski");
        c.setDecidedAt(t);
        c.setAcceptedLeadId(42L);

        LeadCandidateDto d = LeadCandidateDto.from(c);

        assertEquals("jan.kowalski", d.decidedBy());
        assertEquals(t, d.decidedAt());
        assertEquals(42L, d.acceptedLeadId());
        assertEquals("ACCEPTED", d.status());
    }

    @Test
    void odrzuconyNiesieKtoIPowod() {
        LeadCandidate c = new LeadCandidate();
        c.setStatus(LeadCandidateStatus.REJECTED);
        c.setDecidedBy("anna.nowak");
        c.setRejectionNote("nie nasz segment");

        LeadCandidateDto d = LeadCandidateDto.from(c);

        assertEquals("anna.nowak", d.decidedBy());
        assertEquals("nie nasz segment", d.rejectionNote());
    }

    @Test
    void nowyNieMaDecyzji() {
        LeadCandidate c = new LeadCandidate();
        c.setStatus(LeadCandidateStatus.NEW);

        LeadCandidateDto d = LeadCandidateDto.from(c);

        assertNull(d.decidedBy());
        assertNull(d.decidedAt());
        assertNull(d.rejectionNote());
    }
}
