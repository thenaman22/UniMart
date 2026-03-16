package com.unimart.service;

import com.unimart.domain.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

    private final long maxFileSizeBytes;
    private final String[] allowedImageTypes;
    private final String[] allowedVideoTypes;
    private final Path storageRoot;
    private final Path bundledMediaRoot;

    public UploadService(
        @Value("${app.media.max-file-size-bytes}") long maxFileSizeBytes,
        @Value("${app.media.allowed-image-types}") String allowedImageTypes,
        @Value("${app.media.allowed-video-types}") String allowedVideoTypes,
        @Value("${app.media.storage-path}") String storagePath
    ) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.allowedImageTypes = allowedImageTypes.split(",");
        this.allowedVideoTypes = allowedVideoTypes.split(",");
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        this.bundledMediaRoot = Paths.get("ImagesData").toAbsolutePath().normalize();
    }

    public Map<String, Object> prepareUpload(String contentType, long fileSize) {
        MediaType mediaType = validate(contentType, fileSize);
        String storageKey = UUID.randomUUID() + extensionFromContentType(contentType);
        return Map.of(
            "storageKey", storageKey,
            "contentType", contentType,
            "fileSize", fileSize,
            "mediaType", mediaType.name(),
            "uploadUrl", "/media/" + storageKey
        );
    }

    public Map<String, Object> storeFile(MultipartFile file) {
        try {
            Files.createDirectories(storageRoot);
            String contentType = file.getContentType();
            long fileSize = file.getSize();
            MediaType mediaType = validate(contentType, fileSize);
            String storageKey = UUID.randomUUID() + extensionFromFile(file.getOriginalFilename(), contentType);
            Path target = storageRoot.resolve(storageKey).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid storage path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return Map.of(
                "storageKey", storageKey,
                "contentType", contentType,
                "fileSize", fileSize,
                "mediaType", mediaType.name(),
                "url", "/media/" + storageKey
            );
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file");
        }
    }

    public Path resolvePath(String storageKey) {
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        if (Files.exists(resolved)) {
            return resolved;
        }
        Path recovered = recoverBundledMedia(storageKey);
        if (recovered != null) {
            return recovered;
        }
        return resolved;
    }

    public String mediaUrl(String storageKey) {
        return storageKey == null || storageKey.isBlank() ? "" : "/media/" + storageKey;
    }

    private MediaType validate(String contentType, long fileSize) {
        if (contentType == null || contentType.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing media content type");
        }
        if (fileSize > maxFileSizeBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File exceeds maximum allowed size");
        }
        if (Arrays.asList(allowedImageTypes).contains(contentType)) {
            return MediaType.IMAGE;
        }
        if (Arrays.asList(allowedVideoTypes).contains(contentType)) {
            return MediaType.VIDEO;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported media type");
    }

    private String extensionFromFile(String originalName, String contentType) {
        if (originalName != null) {
            int index = originalName.lastIndexOf('.');
            if (index >= 0) {
                return originalName.substring(index);
            }
        }
        return extensionFromContentType(contentType);
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            default -> "";
        };
    }

    private Path recoverBundledMedia(String storageKey) {
        try {
            Files.createDirectories(storageRoot);
            String normalizedKey = storageKey.replace('\\', '/');
            String fileName = normalizedKey.substring(normalizedKey.lastIndexOf('/') + 1).trim();
            if (fileName.isBlank()) {
                return null;
            }
            Path source = bundledMediaRoot.resolve(fileName).normalize();
            if (!source.startsWith(bundledMediaRoot) || !Files.exists(source)) {
                return null;
            }
            Path target = storageRoot.resolve(fileName).normalize();
            if (!target.startsWith(storageRoot)) {
                return null;
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException exception) {
            return null;
        }
    }
}
