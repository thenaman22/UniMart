package com.unimart.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MessageDtos {
    public record SendMessageRequest(@NotBlank @Size(max = 2000) String body) {}
}
