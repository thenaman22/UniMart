package com.unimart.api.dto;

import com.unimart.domain.MediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class MessageDtos {
    public record SendMessageRequest(
        @Size(max = 2000) String body,
        @Valid MessageAttachmentRequest attachment
    ) {}

    public record MessageAttachmentRequest(
        @NotBlank String storageKey,
        @NotBlank String contentType,
        @Positive long fileSize,
        @NotNull MediaType mediaType
    ) {}
}
