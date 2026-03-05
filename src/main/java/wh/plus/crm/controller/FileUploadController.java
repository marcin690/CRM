package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wh.plus.crm.service.MinioUploadService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class FileUploadController {

    private final MinioUploadService minioUploadService;

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String url = minioUploadService.uploadFile(file);
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("fileName", file.getOriginalFilename());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/multiple")
    public ResponseEntity<List<Map<String, String>>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files) {
        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = minioUploadService.uploadFile(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("fileName", file.getOriginalFilename());
            results.add(result);
        }
        return ResponseEntity.ok(results);
    }
}
