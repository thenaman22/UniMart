package com.unimart.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class UploadDtos {
    public record PrepareUploadRequest(
        @NotBlank String contentType,
        @Min(1) long fileSize
    ) {}
}
