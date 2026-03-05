package wh.plus.crm.dto.offer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamOfferStatisticsDTO {

    private Long teamId;
    private String teamName;
    private Long totalOffers;
    private Long convertedOffers;
    private Double conversionRate;
    private BigDecimal totalValue;
    private BigDecimal convertedValue;
    private BigDecimal averageOfferValue;
}
