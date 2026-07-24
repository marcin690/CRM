package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.sharepoint.SharePointItemDTO;
import wh.plus.crm.service.sharepoint.SharePointService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sharepoint")
@RequiredArgsConstructor
public class SharePointController {

    private final SharePointService service;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("configured", service.isConfigured()));
    }

    @GetMapping("/files")
    public ResponseEntity<List<SharePointItemDTO>> browse(@RequestParam(required = false) String path) {
        return ResponseEntity.ok(service.browse(path));
    }
}
