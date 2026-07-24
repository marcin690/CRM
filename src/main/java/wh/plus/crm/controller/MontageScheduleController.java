package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wh.plus.crm.dto.montage.MontageDTO;
import wh.plus.crm.service.MontageService;

import java.util.List;

/** Globalny harmonogram montaży (wszystkie projekty) — podstawa widoku Gantt. */
@RestController
@RequestMapping("/montages")
@RequiredArgsConstructor
public class MontageScheduleController {

    private final MontageService service;

    @GetMapping("/schedule")
    public ResponseEntity<List<MontageDTO>> schedule() {
        return ResponseEntity.ok(service.schedule());
    }
}
