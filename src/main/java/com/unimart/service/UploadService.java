package com.unimart.service;

import com.unimart.domain.MediaType;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UploadService {

    private final long maxFileSizeBytes;
    private final String[] allowedImageTypes;
    private final String[] allowedVideoTypes;

    public UploadService(
        @Value("${app.media.max-file-size-bytes}") long maxFileSizeBytes,
        @Value("${app.media.allowed-image-types}") String allowedImageTypes,
        @Value("${app.media.allowed-video-types}") String allowedVideoTypes
    ) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.allowedImageTypes = allowedImageTypes.split(",");
        this.allowedVideoTypes = allowedVideoTypes.split(",");
    }

    public Map<String, Object> prepareUpload(String contentType, long fileSize) {
        if (fileSize > maxFileSizeBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File exceeds maximum allowed size");
        }
        MediaType mediaType;
        if (Arrays.asList(allowedImageTypes).contains(contentType)) {
            mediaType = MediaType.IMAGE;
        } else if (Arrays.asList(allowedVideoTypes).contains(contentType)) {
            mediaType = MediaType.VIDEO;
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported media type");
        }

        String storageKey = UUID.randomUUID() + "-" + contentType.replace('/', '-');
        return Map.of(
            "storageKey", storageKey,
            "contentType", contentType,
            "fileSize", fileSize,
            "mediaType", mediaType.name(),
            "uploadUrl", "https://storage.example.com/upload/" + storageKey
        );
    }
}
