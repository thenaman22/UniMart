package com.unimart.api;

import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.MembershipStatus;
import com.unimart.domain.UserAccount;
import com.unimart.repository.MembershipRepository;
import com.unimart.repository.ReportRepository;
import com.unimart.service.ApiException;
import com.unimart.service.MembershipService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moderation")
public class ModerationController {
    private static final List<MembershipRole> MODERATION_ROLES = List.of(MembershipRole.ADMIN, MembershipRole.MODERATOR);

    private final MembershipService membershipService;
    private final MembershipRepository membershipRepository;
    private final ReportRepository reportRepository;

    public ModerationController(
        MembershipService membershipService,
        MembershipRepository membershipRepository,
        ReportRepository reportRepository
    ) {
        this.membershipService = membershipService;
        this.membershipRepository = membershipRepository;
        this.reportRepository = reportRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@CurrentUser AuthContext authContext) {
        UserAccount user = requireAuth(authContext);
        List<Membership> manageableMemberships = membershipRepository.findByUserIdAndStatusAndRoleIn(
            user.getId(),
            MembershipStatus.ACTIVE,
            MODERATION_ROLES
        );

        if (manageableMemberships.isEmpty()) {
            return Map.of(
                "pendingRequestCount", 0,
                "communities", List.of()
            );
        }

        List<Long> communityIds = manageableMemberships.stream()
            .map(membership -> membership.getCommunity().getId())
            .distinct()
            .sorted()
            .toList();

        Map<Long, Long> pendingCountsByCommunityId = membershipRepository
            .findPendingRequestCountsByCommunityIds(communityIds, MembershipStatus.PENDING)
            .stream()
            .collect(Collectors.toMap(
                MembershipRepository.PendingRequestCountView::getCommunityId,
                MembershipRepository.PendingRequestCountView::getPendingRequestCount
            ));

        List<Map<String, Object>> communities = communityIds.stream()
            .map(communityId -> {
                LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
                payload.put("communityId", communityId);
                payload.put("pendingRequestCount", Math.toIntExact(pendingCountsByCommunityId.getOrDefault(communityId, 0L)));
                return (Map<String, Object>) payload;
            })
            .toList();

        int pendingRequestCount = communities.stream()
            .map(item -> (Integer) item.get("pendingRequestCount"))
            .mapToInt(Integer::intValue)
            .sum();

        return Map.of(
            "pendingRequestCount", pendingRequestCount,
            "communities", communities
        );
    }

    @GetMapping("/{communityId}/requests")
    public List<Map<String, Object>> membershipRequests(@PathVariable Long communityId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        membershipService.requireModerator(authContext.user().getId(), communityId);
        return membershipRepository.findByCommunityIdAndStatus(communityId, MembershipStatus.PENDING).stream()
            .map(Mapper::membershipRequest)
            .toList();
    }

    @GetMapping("/{communityId}/members")
    public List<Map<String, Object>> activeMembers(@PathVariable Long communityId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        membershipService.requireModerator(authContext.user().getId(), communityId);
        return membershipRepository.findByCommunityIdAndStatus(communityId, MembershipStatus.ACTIVE).stream()
            .sorted(Comparator.comparing(membership -> membership.getUser().getDisplayName().toLowerCase()))
            .map(Mapper::activeMember)
            .toList();
    }

    @PatchMapping("/memberships/{membershipId}")
    public Map<String, Object> updateMembership(
        @PathVariable Long membershipId,
        @RequestParam boolean approve,
        @CurrentUser AuthContext authContext
    ) {
        requireAuth(authContext);
        Membership membership = approve
            ? membershipService.approveMembership(authContext.user().getId(), membershipId)
            : membershipService.revokeMembership(authContext.user().getId(), membershipId);
        return Mapper.community(membership);
    }

    @GetMapping("/{communityId}/reports")
    public List<Map<String, Object>> reports(@PathVariable Long communityId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        membershipService.requireModerator(authContext.user().getId(), communityId);
        return reportRepository.findByListingCommunityIdAndResolvedFalse(communityId).stream()
            .map(Mapper::report)
            .toList();
    }

    private UserAccount requireAuth(AuthContext authContext) {
        if (authContext == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authContext.user();
    }
}
