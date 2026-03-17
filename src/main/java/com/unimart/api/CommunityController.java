package com.unimart.api;

import com.unimart.api.dto.CommunityDtos.AddDomainRequest;
import com.unimart.api.dto.CommunityDtos.CreateInviteRequest;
import com.unimart.api.dto.CommunityDtos.JoinByInviteRequest;
import com.unimart.domain.Community;
import com.unimart.domain.Membership;
import com.unimart.repository.CommunityRepository;
import com.unimart.service.ApiException;
import com.unimart.service.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/communities")
public class CommunityController {

    private final MembershipService membershipService;
    private final CommunityRepository communityRepository;

    public CommunityController(MembershipService membershipService, CommunityRepository communityRepository) {
        this.membershipService = membershipService;
        this.communityRepository = communityRepository;
    }

    @GetMapping
    public List<Map<String, Object>> myCommunities(@CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        return membershipService.activeMemberships(authContext.user()).stream()
            .map(Mapper::community)
            .toList();
    }

    @GetMapping("/discover")
    public List<Map<String, Object>> discoverCommunities() {
        return communityRepository.findAll().stream().map(this::toCommunityCard).toList();
    }

    @GetMapping("/{communityId}")
    public Map<String, Object> communityDetail(@PathVariable Long communityId, @CurrentUser AuthContext authContext) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Community not found"));

        Map<String, Object> payload = toCommunityCard(community);
        if (authContext == null) {
            return payload;
        }

        return membershipService.activeMemberships(authContext.user()).stream()
            .filter(membership -> membership.getCommunity().getId().equals(communityId))
            .findFirst()
            .<Map<String, Object>>map(membership -> Map.of(
                "id", community.getId(),
                "slug", community.getSlug(),
                "name", community.getName(),
                "description", community.getDescription(),
                "privateCommunity", community.isPrivateCommunity(),
                "membership", Map.of(
                    "membershipId", membership.getId(),
                    "role", membership.getRole().name(),
                    "status", membership.getStatus().name()
                )
            ))
            .orElse(payload);
    }

    @PostMapping("/{communityId}/join-by-domain")
    public Map<String, Object> joinByDomain(@PathVariable Long communityId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        Membership membership = membershipService.joinByDomain(authContext.user(), communityId);
        return Mapper.community(membership);
    }

    @PostMapping("/{communityId}/request")
    public Map<String, Object> requestMembership(@PathVariable Long communityId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        Membership membership = membershipService.requestMembership(authContext.user(), communityId);
        return Mapper.community(membership);
    }

    @DeleteMapping("/{communityId}/membership")
    public Map<String, Object> leaveCommunity(@PathVariable Long communityId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        Membership membership = membershipService.leaveCommunity(authContext.user(), communityId);
        return Mapper.community(membership);
    }

    @PostMapping("/join-by-invite")
    public Map<String, Object> joinByInvite(@Valid @RequestBody JoinByInviteRequest request, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        Membership membership = membershipService.joinByInvite(authContext.user(), request.token());
        return Mapper.community(membership);
    }

    @PostMapping("/{communityId}/invites")
    public Map<String, Object> createInvite(
        @PathVariable Long communityId,
        @Valid @RequestBody CreateInviteRequest request,
        @CurrentUser AuthContext authContext
    ) {
        requireAuth(authContext);
        return Mapper.invite(membershipService.createInvite(authContext.user().getId(), communityId, request.maxUses()));
    }

    @PostMapping("/{communityId}/domains")
    public Map<String, Object> addDomain(
        @PathVariable Long communityId,
        @Valid @RequestBody AddDomainRequest request,
        @CurrentUser AuthContext authContext
    ) {
        requireAuth(authContext);
        return Map.of(
            "communityId", communityId,
            "emailDomain", membershipService.addDomain(authContext.user().getId(), communityId, request.emailDomain()).getEmailDomain()
        );
    }

    private Map<String, Object> toCommunityCard(Community community) {
        return Map.of(
            "id", community.getId(),
            "slug", community.getSlug(),
            "name", community.getName(),
            "description", community.getDescription(),
            "privateCommunity", community.isPrivateCommunity()
        );
    }

    private void requireAuth(AuthContext authContext) {
        if (authContext == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
