package com.unimart.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record SignUpRequest(
        @NotBlank String displayName,
        @NotBlank @Email String email
    ) {}

    public record RequestCodeRequest(
        @NotBlank @Email String email
    ) {}

    public record VerifyCodeRequest(
        @NotBlank @Email String email,
        @NotBlank String code
    ) {}
}
