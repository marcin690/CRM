package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.offer.OfferStatisticsDTO;
import wh.plus.crm.service.OfferStatisticsService;

import java.time.LocalDate;

@RestController
@RequestMapping("/offers/statistics")
@RequiredArgsConstructor
public class OfferStatisticsController {

    private final OfferStatisticsService offerStatisticsService;

    @GetMapping
    public ResponseEntity<OfferStatisticsDTO> getOfferStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        OfferStatisticsDTO statistics = offerStatisticsService.getOfferStatistics(dateFrom, dateTo);
        return ResponseEntity.ok(statistics);
    }
}
