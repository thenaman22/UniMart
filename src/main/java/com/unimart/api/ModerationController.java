package com.unimart.api;

import com.unimart.domain.Membership;
import com.unimart.domain.MembershipStatus;
import com.unimart.repository.MembershipRepository;
import com.unimart.repository.ReportRepository;
import com.unimart.service.ApiException;
import com.unimart.service.MembershipService;
import java.util.List;
import java.util.Map;
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

    @GetMapping("/{communityId}/requests")
    public List<Map<String, Object>> membershipRequests(@PathVariable Long communityId, @CurrentUser AuthContext authContext) {
        requireAuth(authContext);
        membershipService.requireModerator(authContext.user().getId(), communityId);
        return membershipRepository.findByCommunityIdAndStatus(communityId, MembershipStatus.PENDING).stream()
            .map(Mapper::membershipRequest)
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

    private void requireAuth(AuthContext authContext) {
        if (authContext == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
