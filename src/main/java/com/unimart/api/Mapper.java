package com.unimart.api;

import com.unimart.domain.Community;
import com.unimart.domain.InviteLink;
import com.unimart.domain.Listing;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.Membership;
import com.unimart.domain.Report;
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

    public static Map<String, Object> listingSummary(Listing listing) {
        return Map.of(
            "id", listing.getId(),
            "communityId", listing.getCommunity().getId(),
            "title", listing.getTitle(),
            "description", listing.getDescription(),
            "price", listing.getPrice(),
            "category", listing.getCategory(),
            "itemCondition", listing.getItemCondition(),
            "status", listing.getStatus().name(),
            "sellerName", listing.getSeller().getDisplayName(),
            "createdAt", listing.getCreatedAt()
        );
    }

    public static Map<String, Object> listingDetail(Listing listing, List<ListingMedia> media) {
        return Map.of(
            "listing", listingSummary(listing),
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
            "url", "https://storage.example.com/" + listingMedia.getStorageKey()
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
}
