package wh.plus.crm.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wnioski AI (Claude) do cockpitu sprzedaży: podsumowanie + listy wniosków, rekomendacji i ryzyk.
 * error != null oznacza, że nie udało się wygenerować (np. brak klucza) — front pokaże komunikat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesInsightsDTO {
    private String summary;
    private List<String> wnioski;
    private List<String> rekomendacje;
    private List<String> ryzyka;
    private String model;
    private String generatedAt;
    private String error;
}
