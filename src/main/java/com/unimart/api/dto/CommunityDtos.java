package com.unimart.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CommunityDtos {
    public record JoinByInviteRequest(@NotBlank String token) {}
    public record CreateInviteRequest(@Min(1) int maxUses) {}
    public record AddDomainRequest(@NotBlank String emailDomain) {}
}
