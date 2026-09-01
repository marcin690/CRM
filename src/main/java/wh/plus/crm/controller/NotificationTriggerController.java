package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.notification.NotificationTriggerDTO;
import wh.plus.crm.model.notification.NotificationTriggerType;
import wh.plus.crm.service.notification.NotificationTriggerService;

import java.util.List;

/**
 * Panel admina: konfiguracja triggerów powiadomień (kto, jakie kanały, on/off).
 * hasAuthority('ADMIN') — role w bazie są bez prefiksu ROLE_ (patrz Role.getAuthority()).
 */
@RestController
@RequestMapping("/notifications/triggers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class NotificationTriggerController {

    private final NotificationTriggerService triggerService;

    @GetMapping
    public ResponseEntity<List<NotificationTriggerDTO>> list() {
        return ResponseEntity.ok(triggerService.getAll());
    }

    @PutMapping("/{type}")
    public ResponseEntity<NotificationTriggerDTO> update(
            @PathVariable NotificationTriggerType type,
            @RequestBody NotificationTriggerDTO dto) {
        return ResponseEntity.ok(triggerService.update(type, dto));
    }
}
