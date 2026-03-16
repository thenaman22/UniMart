package com.unimart.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileDtos {
    public record UpdateProfileRequest(
        @NotBlank String displayName,
        @Size(max = 1000) String bio,
        String phoneNumber,
        String location,
        boolean publicPhoneVisible
    ) {}
}
