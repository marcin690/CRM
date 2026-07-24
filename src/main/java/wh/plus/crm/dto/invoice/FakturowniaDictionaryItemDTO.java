package wh.plus.crm.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lekki słownikowy item z Fakturowni — używany do napełniania dropdownów
 * (companies / categories / projects). Pochodzi z odczytu API Fakturowni,
 * nigdy nie jest zapisywany do bazy ani propagowany z powrotem.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FakturowniaDictionaryItemDTO {
    private Long id;
    private String name;
}
