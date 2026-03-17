package com.unimart.service;

import com.unimart.domain.Community;
import com.unimart.domain.CommunityDomain;
import com.unimart.domain.CommunityPostingPolicy;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        if (!canModerate(membership.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Moderator access required");
        }
        return membership;
    }

    public Membership requireAdmin(Long userId, Long communityId) {
        Membership membership = requireActiveMembership(userId, communityId);
        if (membership.getRole() != MembershipRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        return membership;
    }

    public Membership requireCanPost(Long userId, Long communityId) {
        Membership membership = requireActiveMembership(userId, communityId);
        if (!canPost(membership)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot post listings in this community");
        }
        return membership;
    }

    public List<Membership> activeMemberships(UserAccount user) {
        return membershipRepository.findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE);
    }

    public List<Long> activeCommunityIds(UserAccount user) {
        return activeMemberships(user).stream()
            .map(membership -> membership.getCommunity().getId())
            .toList();
    }

    public List<Long> sharedActiveCommunityIds(UserAccount firstUser, UserAccount secondUser) {
        Set<Long> firstIds = new HashSet<>(activeCommunityIds(firstUser));
        if (firstIds.isEmpty()) {
            return List.of();
        }
        return activeCommunityIds(secondUser).stream()
            .filter(firstIds::contains)
            .toList();
    }

    @Transactional
    public List<Membership> autoAssignMembershipsForUser(UserAccount user) {
        String emailDomain = user.getEmail().substring(user.getEmail().indexOf('@') + 1);
        List<CommunityDomain> matches = communityDomainRepository.findByEmailDomain(emailDomain);
        return matches.stream()
            .map(match -> upsertMembership(user, match.getCommunity(), MembershipStatus.ACTIVE))
            .toList();
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
    public Membership leaveCommunity(UserAccount user, Long communityId) {
        Membership membership = membershipRepository.findByUserIdAndCommunityId(user.getId(), communityId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only active memberships can be left");
        }
        if (isCommunityCreator(membership) || membership.getRole() == MembershipRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admins must delete the community instead of leaving it");
        }
        membership.setStatus(MembershipStatus.REVOKED);
        return membership;
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
        Membership actor = requireModerator(moderatorUserId, membership.getCommunity().getId());
        if (membership.getUser().getId().equals(actor.getUser().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use the leave community flow to remove yourself");
        }
        if (isCommunityCreator(membership)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "The community creator cannot be removed");
        }
        if (actor.getRole() == MembershipRole.MODERATOR && canModerate(membership.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Moderators cannot remove staff members");
        }
        membership.setStatus(MembershipStatus.REVOKED);
        return membership;
    }

    @Transactional
    public InviteLink createInvite(Long moderatorUserId, Long communityId, int maxUses) {
        requireAdmin(moderatorUserId, communityId);
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
        requireAdmin(moderatorUserId, communityId);
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Community not found"));
        CommunityDomain domain = new CommunityDomain();
        domain.setCommunity(community);
        domain.setEmailDomain(emailDomain);
        return communityDomainRepository.save(domain);
    }

    @Transactional
    public Membership updateRole(Long adminUserId, Long communityId, Long membershipId, MembershipRole role) {
        requireAdmin(adminUserId, communityId);
        Membership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
        if (!Objects.equals(membership.getCommunity().getId(), communityId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership does not belong to this community");
        }
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only active members can have their role updated");
        }
        if (role == MembershipRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admin role cannot be reassigned");
        }
        if (isCommunityCreator(membership)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "The community creator role cannot be changed");
        }
        if (role == MembershipRole.SELLER
            && membership.getCommunity().getPostingPolicy() != CommunityPostingPolicy.APPROVED_SELLERS_ONLY) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Seller role is only available in communities that require approved sellers"
            );
        }
        membership.setRole(role);
        return membership;
    }

    public boolean canPost(Membership membership) {
        CommunityPostingPolicy postingPolicy = membership.getCommunity().getPostingPolicy();
        return switch (postingPolicy) {
            case ALL_MEMBERS_CAN_POST -> true;
            case APPROVED_SELLERS_ONLY -> membership.getRole() == MembershipRole.ADMIN
                || membership.getRole() == MembershipRole.MODERATOR
                || membership.getRole() == MembershipRole.SELLER;
            case CREATOR_ONLY -> isCommunityCreator(membership);
        };
    }

    public boolean canModerate(MembershipRole role) {
        return role == MembershipRole.ADMIN || role == MembershipRole.MODERATOR;
    }

    public boolean isCommunityCreator(Membership membership) {
        Community community = membership.getCommunity();
        return community.getCreator() != null && community.getCreator().getId().equals(membership.getUser().getId());
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
