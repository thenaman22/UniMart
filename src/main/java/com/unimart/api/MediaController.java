package com.unimart.api;

import com.unimart.service.UploadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final UploadService uploadService;

    public MediaController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @GetMapping("/{storageKey:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String storageKey) throws IOException {
        Path path = uploadService.resolvePath(storageKey);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(path);
        if (contentType == null || MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
            contentType = fallbackContentType(storageKey);
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType)
            .body(new FileSystemResource(path));
    }

    private String fallbackContentType(String storageKey) {
        String lower = storageKey.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".jfif")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (lower.endsWith(".webm")) {
            return "video/webm";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
