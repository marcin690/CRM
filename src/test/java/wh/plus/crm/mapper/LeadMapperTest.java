package wh.plus.crm.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import wh.plus.crm.dto.lead.LeadSummaryDTO;
import wh.plus.crm.model.lead.Lead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LeadMapperTest {

    private final LeadMapper leadMapper = Mappers.getMapper(LeadMapper.class);

    @Test
    void testLeadToLeadSummaryDTO() {
        // Arrange: Przygotuj obiekt Lead
        Lead lead = new Lead();
        lead.setId(1L);
        lead.setName("Test Lead");

        // Act: Mapuj Lead na LeadSummaryDTO
        LeadSummaryDTO dto = leadMapper.toLeadSummaryDTO(lead);

        // Assert: Sprawdź wynik mapowania
        assertNotNull(dto, "LeadSummaryDTO should not be null");
        assertEquals(1L, dto.getId(), "ID should match");
        assertEquals("Test Lead", dto.getName(), "Name should match");
    }


}
