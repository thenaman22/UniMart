package com.unimart.api;

import com.unimart.domain.Community;
import com.unimart.domain.CommunityPostingPolicy;
import com.unimart.domain.InviteLink;
import com.unimart.domain.Listing;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.Report;
import com.unimart.domain.UserAccount;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Mapper {

    private Mapper() {
    }

    public static Map<String, Object> community(Membership membership) {
        return community(membership.getCommunity(), membership);
    }

    public static Map<String, Object> community(Community community, Membership membership) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", community.getId());
        payload.put("communityId", community.getId());
        payload.put("slug", community.getSlug());
        payload.put("name", community.getName());
        payload.put("description", community.getDescription());
        payload.put("privateCommunity", community.isPrivateCommunity());
        payload.put("postingPolicy", community.getPostingPolicy().name());
        payload.put("postingPolicyLabel", postingPolicyLabel(community.getPostingPolicy()));
        payload.put("creatorUserId", community.getCreator() == null ? null : community.getCreator().getId());

        if (membership == null) {
            payload.put("canPost", false);
            payload.put("canModerate", false);
            payload.put("canManageRoles", false);
            payload.put("canManageCommunity", false);
            payload.put("canDelete", false);
            payload.put("isCreator", false);
            return payload;
        }

        payload.put("membershipId", membership.getId());
        payload.put("role", membership.getRole().name());
        payload.put("roleLabel", roleLabel(membership.getRole()));
        payload.put("status", membership.getStatus().name());
        payload.put("isCreator", isCreator(membership));
        payload.put("canPost", canPost(membership));
        payload.put("canModerate", canModerate(membership.getRole()));
        payload.put("canManageRoles", membership.getRole() == MembershipRole.ADMIN);
        payload.put("canManageCommunity", membership.getRole() == MembershipRole.ADMIN);
        payload.put("canDelete", membership.getRole() == MembershipRole.ADMIN);
        return payload;
    }

    public static Map<String, Object> listingSummary(Listing listing, List<ListingMedia> media) {
        ListingMedia previewMedia = media.isEmpty() ? null : media.get(0);
        return Map.ofEntries(
            Map.entry("id", listing.getId()),
            Map.entry("communityId", listing.getCommunity().getId()),
            Map.entry("sellerId", listing.getSeller().getId()),
            Map.entry("title", listing.getTitle()),
            Map.entry("description", listing.getDescription()),
            Map.entry("price", listing.getPrice()),
            Map.entry("category", listing.getCategory()),
            Map.entry("itemCondition", listing.getItemCondition()),
            Map.entry("status", listing.getStatus().name()),
            Map.entry("sellerName", listing.getSeller().getDisplayName()),
            Map.entry("createdAt", listing.getCreatedAt()),
            Map.entry("previewMediaUrl", previewMedia == null ? "" : mediaUrl(previewMedia)),
            Map.entry("previewMediaType", previewMedia == null ? "" : previewMedia.getType().name()),
            Map.entry("media", media.stream().map(Mapper::listingMedia).toList())
        );
    }

    public static Map<String, Object> listingDetail(Listing listing, List<ListingMedia> media) {
        return Map.of(
            "listing", listingSummary(listing, media),
            "media", media.stream().map(Mapper::listingMedia).toList()
        );
    }

    public static Map<String, Object> listingMedia(ListingMedia listingMedia) {
        return Map.of(
            "id", listingMedia.getId(),
            "type", listingMedia.getType().name(),
            "storageKey", listingMedia.getStorageKey(),
            "contentType", listingMedia.getContentType(),
            "fileSize", listingMedia.getFileSize(),
            "url", mediaUrl(listingMedia)
        );
    }

    public static Map<String, Object> invite(InviteLink inviteLink) {
        return Map.of(
            "id", inviteLink.getId(),
            "token", inviteLink.getToken(),
            "expiresAt", inviteLink.getExpiresAt(),
            "maxUses", inviteLink.getMaxUses(),
            "usedCount", inviteLink.getUsedCount()
        );
    }

    public static Map<String, Object> report(Report report) {
        return Map.of(
            "id", report.getId(),
            "listingId", report.getListing().getId(),
            "reporterName", report.getReporter().getDisplayName(),
            "reason", report.getReason(),
            "resolved", report.isResolved(),
            "createdAt", report.getCreatedAt()
        );
    }

    public static Map<String, Object> membershipRequest(Membership membership) {
        return Map.of(
            "membershipId", membership.getId(),
            "communityId", membership.getCommunity().getId(),
            "communityName", membership.getCommunity().getName(),
            "userId", membership.getUser().getId(),
            "requesterName", membership.getUser().getDisplayName(),
            "requesterEmail", membership.getUser().getEmail(),
            "status", membership.getStatus().name(),
            "role", membership.getRole().name(),
            "roleLabel", roleLabel(membership.getRole()),
            "isCreator", isCreator(membership)
        );
    }

    public static Map<String, Object> activeMember(Membership membership) {
        return Map.of(
            "membershipId", membership.getId(),
            "communityId", membership.getCommunity().getId(),
            "communityName", membership.getCommunity().getName(),
            "userId", membership.getUser().getId(),
            "memberName", membership.getUser().getDisplayName(),
            "memberEmail", membership.getUser().getEmail(),
            "status", membership.getStatus().name(),
            "role", membership.getRole().name(),
            "roleLabel", roleLabel(membership.getRole()),
            "isCreator", isCreator(membership)
        );
    }

    public static Map<String, Object> selfProfile(UserAccount user) {
        return Map.of(
            "id", user.getId(),
            "displayName", user.getDisplayName(),
            "email", user.getEmail(),
            "bio", user.getBio() == null ? "" : user.getBio(),
            "phoneNumber", user.getPhoneNumber() == null ? "" : user.getPhoneNumber(),
            "location", user.getLocation() == null ? "" : user.getLocation(),
            "publicPhoneVisible", user.isPublicPhoneVisible(),
            "emailVerified", user.isEmailVerified(),
            "profileImageUrl", user.getProfileImageKey() == null ? "" : "/media/" + user.getProfileImageKey()
        );
    }

    public static Map<String, Object> publicProfile(UserAccount user) {
        return Map.of(
            "id", user.getId(),
            "displayName", user.getDisplayName(),
            "email", user.getEmail(),
            "bio", user.getBio() == null ? "" : user.getBio(),
            "phoneNumber", user.isPublicPhoneVisible() && user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
            "location", user.getLocation() == null ? "" : user.getLocation(),
            "profileImageUrl", user.getProfileImageKey() == null ? "" : "/media/" + user.getProfileImageKey()
        );
    }

    private static String mediaUrl(ListingMedia listingMedia) {
        return "/media/" + listingMedia.getStorageKey();
    }

    private static String roleLabel(MembershipRole role) {
        return switch (role) {
            case MEMBER -> "Viewer";
            case SELLER -> "Seller";
            case MODERATOR -> "Moderator";
            case ADMIN -> "Admin";
        };
    }

    private static String postingPolicyLabel(CommunityPostingPolicy postingPolicy) {
        return switch (postingPolicy) {
            case ALL_MEMBERS_CAN_POST -> "All members can post";
            case APPROVED_SELLERS_ONLY -> "Approved sellers only";
            case CREATOR_ONLY -> "Creator only";
        };
    }

    private static boolean canModerate(MembershipRole role) {
        return role == MembershipRole.ADMIN || role == MembershipRole.MODERATOR;
    }

    private static boolean canPost(Membership membership) {
        return switch (membership.getCommunity().getPostingPolicy()) {
            case ALL_MEMBERS_CAN_POST -> true;
            case APPROVED_SELLERS_ONLY -> membership.getRole() == MembershipRole.ADMIN
                || membership.getRole() == MembershipRole.MODERATOR
                || membership.getRole() == MembershipRole.SELLER;
            case CREATOR_ONLY -> isCreator(membership);
        };
    }

    private static boolean isCreator(Membership membership) {
        return membership.getCommunity().getCreator() != null
            && membership.getCommunity().getCreator().getId().equals(membership.getUser().getId());
    }
}
