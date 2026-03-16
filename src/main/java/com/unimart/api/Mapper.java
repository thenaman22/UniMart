package com.unimart.api;

import com.unimart.domain.Community;
import com.unimart.domain.InviteLink;
import com.unimart.domain.Listing;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.Membership;
import com.unimart.domain.Report;
import com.unimart.domain.UserAccount;
import java.util.List;
import java.util.Map;

public final class Mapper {

    private Mapper() {
    }

    public static Map<String, Object> community(Membership membership) {
        Community community = membership.getCommunity();
        return Map.of(
            "membershipId", membership.getId(),
            "communityId", community.getId(),
            "slug", community.getSlug(),
            "name", community.getName(),
            "description", community.getDescription(),
            "role", membership.getRole().name(),
            "status", membership.getStatus().name()
        );
    }

    public static Map<String, Object> listingSummary(Listing listing, List<ListingMedia> media) {
        ListingMedia previewMedia = media.isEmpty() ? null : media.get(0);
        return Map.ofEntries(
            Map.entry("id", listing.getId()),
            Map.entry("communityId", listing.getCommunity().getId()),
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
            "requesterName", membership.getUser().getDisplayName(),
            "requesterEmail", membership.getUser().getEmail(),
            "status", membership.getStatus().name(),
            "role", membership.getRole().name()
        );
    }

    public static Map<String, Object> profile(UserAccount user) {
        return Map.of(
            "id", user.getId(),
            "displayName", user.getDisplayName(),
            "email", user.getEmail(),
            "bio", user.getBio() == null ? "" : user.getBio(),
            "phoneNumber", user.getPhoneNumber() == null ? "" : user.getPhoneNumber(),
            "location", user.getLocation() == null ? "" : user.getLocation(),
            "emailVerified", user.isEmailVerified(),
            "profileImageUrl", user.getProfileImageKey() == null ? "" : "/media/" + user.getProfileImageKey()
        );
    }

    private static String mediaUrl(ListingMedia listingMedia) {
        return "/media/" + listingMedia.getStorageKey();
    }
}
