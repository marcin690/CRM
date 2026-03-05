package wh.plus.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.dto.offer.OfferStatisticsDTO;
import wh.plus.crm.dto.offer.TeamOfferStatisticsDTO;
import wh.plus.crm.repository.OfferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferStatisticsService {

    private final OfferRepository offerRepository;

    public OfferStatisticsDTO getOfferStatistics(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        // Statystyki ogólne
        List<Object[]> overallResults = offerRepository.getOverallStatistics(since);
        Object[] overall = overallResults.isEmpty()
                ? new Object[]{0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO}
                : overallResults.get(0);
        Long totalOffers = overall[0] != null ? ((Number) overall[0]).longValue() : 0L;
        Long convertedOffers = overall[1] != null ? ((Number) overall[1]).longValue() : 0L;
        BigDecimal totalValue = overall[2] != null ? new BigDecimal(overall[2].toString()) : BigDecimal.ZERO;
        BigDecimal convertedValue = overall[3] != null ? new BigDecimal(overall[3].toString()) : BigDecimal.ZERO;
        BigDecimal averageOfferValue = overall[4] != null ? new BigDecimal(overall[4].toString()) : BigDecimal.ZERO;
        Double conversionRate = totalOffers > 0
                ? (convertedOffers.doubleValue() / totalOffers.doubleValue()) * 100.0
                : 0.0;

        // Statystyki per team
        List<Object[]> teamResults = offerRepository.getStatisticsByTeam(since);
        List<TeamOfferStatisticsDTO> teamStats = teamResults.stream()
                .map(row -> {
                    Long teamId = row[0] != null ? ((Number) row[0]).longValue() : null;
                    String teamName = (String) row[1];
                    Long teamTotal = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                    Long teamConverted = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                    BigDecimal teamValue = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;
                    BigDecimal teamConvertedValue = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
                    BigDecimal teamAvgValue = row[6] != null ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO;
                    Double teamConversionRate = teamTotal > 0
                            ? (teamConverted.doubleValue() / teamTotal.doubleValue()) * 100.0
                            : 0.0;

                    return new TeamOfferStatisticsDTO(
                            teamId, teamName, teamTotal, teamConverted, teamConversionRate,
                            teamValue, teamConvertedValue, teamAvgValue
                    );
                })
                .collect(Collectors.toList());

        return new OfferStatisticsDTO(totalOffers, convertedOffers, conversionRate,
                totalValue, convertedValue, averageOfferValue, teamStats);
    }
}
