package com.unimart.service;

import com.unimart.domain.Community;
import com.unimart.domain.CommunityDomain;
import com.unimart.domain.InviteLink;
import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.MembershipStatus;
import com.unimart.domain.UserAccount;
import com.unimart.repository.CommunityDomainRepository;
import com.unimart.repository.CommunityRepository;
import com.unimart.repository.InviteLinkRepository;
import com.unimart.repository.MembershipRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final CommunityRepository communityRepository;
    private final CommunityDomainRepository communityDomainRepository;
    private final InviteLinkRepository inviteLinkRepository;

    public MembershipService(
        MembershipRepository membershipRepository,
        CommunityRepository communityRepository,
        CommunityDomainRepository communityDomainRepository,
        InviteLinkRepository inviteLinkRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.communityRepository = communityRepository;
        this.communityDomainRepository = communityDomainRepository;
        this.inviteLinkRepository = inviteLinkRepository;
    }

    public Membership requireActiveMembership(Long userId, Long communityId) {
        Membership membership = membershipRepository.findByUserIdAndCommunityId(userId, communityId)
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this community"));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Membership is not active");
        }
        return membership;
    }

    public Membership requireModerator(Long userId, Long communityId) {
        Membership membership = requireActiveMembership(userId, communityId);
        if (membership.getRole() == MembershipRole.MEMBER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Moderator access required");
        }
        return membership;
    }

    public List<Membership> activeMemberships(UserAccount user) {
        return membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE);
    }

    @Transactional
    public Membership joinByDomain(UserAccount user, Long communityId) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Community not found"));
        String emailDomain = user.getEmail().substring(user.getEmail().indexOf('@') + 1);
        boolean eligible = communityDomainRepository.findByEmailDomain(emailDomain).stream()
            .map(CommunityDomain::getCommunity)
            .anyMatch(candidate -> candidate.getId().equals(communityId));
        if (!eligible) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Email domain is not eligible for this community");
        }
        return upsertMembership(user, community, MembershipStatus.ACTIVE);
    }

    @Transactional
    public Membership requestMembership(UserAccount user, Long communityId) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Community not found"));
        return upsertMembership(user, community, MembershipStatus.PENDING);
    }

    @Transactional
    public Membership joinByInvite(UserAccount user, String inviteToken) {
        InviteLink inviteLink = inviteLinkRepository.findByToken(inviteToken)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite link not found"));
        if (inviteLink.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite link expired");
        }
        if (inviteLink.getUsedCount() >= inviteLink.getMaxUses()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite link usage limit reached");
        }
        inviteLink.setUsedCount(inviteLink.getUsedCount() + 1);
        return upsertMembership(user, inviteLink.getCommunity(), MembershipStatus.ACTIVE);
    }

    @Transactional
    public Membership approveMembership(Long moderatorUserId, Long membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership request not found"));
        requireModerator(moderatorUserId, membership.getCommunity().getId());
        membership.setStatus(MembershipStatus.ACTIVE);
        return membership;
    }

    @Transactional
    public Membership revokeMembership(Long moderatorUserId, Long membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
        requireModerator(moderatorUserId, membership.getCommunity().getId());
        membership.setStatus(MembershipStatus.REVOKED);
        return membership;
    }

    @Transactional
    public InviteLink createInvite(Long moderatorUserId, Long communityId, int maxUses) {
        requireModerator(moderatorUserId, communityId);
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Community not found"));
        InviteLink inviteLink = new InviteLink();
        inviteLink.setCommunity(community);
        inviteLink.setToken(UUID.randomUUID().toString());
        inviteLink.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        inviteLink.setMaxUses(maxUses);
        inviteLink.setUsedCount(0);
        return inviteLinkRepository.save(inviteLink);
    }

    @Transactional
    public CommunityDomain addDomain(Long moderatorUserId, Long communityId, String emailDomain) {
        requireModerator(moderatorUserId, communityId);
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Community not found"));
        CommunityDomain domain = new CommunityDomain();
        domain.setCommunity(community);
        domain.setEmailDomain(emailDomain);
        return communityDomainRepository.save(domain);
    }

    private Membership upsertMembership(UserAccount user, Community community, MembershipStatus status) {
        Membership membership = membershipRepository.findByUserIdAndCommunityId(user.getId(), community.getId())
            .orElseGet(Membership::new);
        membership.setUser(user);
        membership.setCommunity(community);
        membership.setStatus(status);
        if (membership.getRole() == null) {
            membership.setRole(MembershipRole.MEMBER);
        }
        return membershipRepository.save(membership);
    }
}
