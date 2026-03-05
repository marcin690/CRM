package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.offer.OfferStatisticsDTO;
import wh.plus.crm.service.OfferStatisticsService;

@RestController
@RequestMapping("/offers/statistics")
@RequiredArgsConstructor
public class OfferStatisticsController {

    private final OfferStatisticsService offerStatisticsService;

    @GetMapping
    public ResponseEntity<OfferStatisticsDTO> getOfferStatistics(
            @RequestParam(defaultValue = "90") int days
    ) {
        OfferStatisticsDTO statistics = offerStatisticsService.getOfferStatistics(days);
        return ResponseEntity.ok(statistics);
    }
}
