package wh.plus.crm.dto.offer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferStatisticsDTO {

    private Long totalOffers;
    private Long convertedOffers;
    private Double conversionRate;
    private BigDecimal totalValue;
    private BigDecimal convertedValue;
    private BigDecimal averageOfferValue;
    private List<TeamOfferStatisticsDTO> teamStatistics;
}
