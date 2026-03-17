package com.unimart.api.dto;

import com.unimart.domain.CommunityPostingPolicy;
import com.unimart.domain.MembershipRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CommunityDtos {
    public record CreateCommunityRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 1000) String description,
        @NotNull CommunityPostingPolicy postingPolicy
    ) {}

    public record JoinByInviteRequest(@NotBlank String token) {}
    public record CreateInviteRequest(@Min(1) int maxUses) {}
    public record AddDomainRequest(@NotBlank String emailDomain) {}
    public record UpdateMembershipRoleRequest(@NotNull MembershipRole role) {}
}
