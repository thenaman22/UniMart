package com.unimart.api;

import com.unimart.domain.Listing;
import com.unimart.domain.ListingConversation;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.ListingMessage;
import com.unimart.domain.UserAccount;
import java.util.LinkedHashMap;
import com.unimart.service.MessagingService;
import java.util.Map;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static Map<String, Object> sellerInboxListing(
        MessagingService.SellerInboxListing listing,
        ListingMedia previewMedia
    ) {
        return Map.of(
            "listing", listingSummary(listing.listing(), previewMedia),
            "lastMessageAt", listing.lastMessageAt(),
            "unreadCount", listing.unreadCount(),
            "conversationCount", listing.conversationCount()
        );
    }

    public static Map<String, Object> sellerConversationSummary(
        MessagingService.ConversationPreview preview
    ) {
        return Map.of(
            "id", preview.conversation().getId(),
            "buyer", participant(preview.conversation().getBuyer()),
            "lastMessageAt", preview.conversation().getLastMessageAt(),
            "unreadCount", preview.unreadCount(),
            "lastMessagePreview", preview.lastMessagePreview()
        );
    }

    public static Map<String, Object> buyerInboxListing(
        MessagingService.BuyerInboxListing listing,
        ListingMedia previewMedia
    ) {
        return Map.of(
            "listing", listingSummary(listing.conversation().getListing(), previewMedia),
            "conversationId", listing.conversation().getId(),
            "seller", participant(listing.conversation().getSeller()),
            "lastMessageAt", listing.conversation().getLastMessageAt(),
            "unreadCount", listing.conversation().getBuyerUnreadCount(),
            "lastMessagePreview", listing.lastMessagePreview()
        );
    }

    public static Map<String, Object> conversationDetail(
        MessagingService.ConversationDetail detail,
        Long currentUserId,
        ListingMedia previewMedia
    ) {
        ListingConversation conversation = detail.conversation();
        return Map.of(
            "conversationId", conversation.getId(),
            "listing", listingSummary(conversation.getListing(), previewMedia),
            "seller", participant(conversation.getSeller()),
            "buyer", participant(conversation.getBuyer()),
            "readOnly", detail.readOnly(),
            "messages", detail.messages().stream()
                .map(item -> message(item, currentUserId))
                .toList()
        );
    }

    public static Map<String, Object> listingContext(Listing listing, ListingMedia previewMedia) {
        return listingSummary(listing, previewMedia);
    }

    public static Map<String, Object> message(ListingMessage message, Long currentUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.getId());
        payload.put("body", message.getBody());
        payload.put("createdAt", message.getCreatedAt());
        payload.put("mine", message.getSender().getId().equals(currentUserId));
        payload.put("sender", participant(message.getSender()));
        payload.put("attachment", message.hasAttachment() ? attachment(message) : null);
        return payload;
    }

    private static Map<String, Object> listingSummary(Listing listing, ListingMedia previewMedia) {
        return Map.ofEntries(
            Map.entry("id", listing.getId()),
            Map.entry("title", listing.getTitle()),
            Map.entry("status", listing.getStatus().name()),
            Map.entry("price", listing.getPrice()),
            Map.entry("category", listing.getCategory()),
            Map.entry("itemCondition", listing.getItemCondition()),
            Map.entry("sellerId", listing.getSeller().getId()),
            Map.entry("sellerName", listing.getSeller().getDisplayName()),
            Map.entry("previewMediaUrl", previewMedia == null ? "" : "/media/" + previewMedia.getStorageKey()),
            Map.entry("previewMediaType", previewMedia == null ? "" : previewMedia.getType().name())
        );
    }

    private static Map<String, Object> participant(UserAccount user) {
        return Map.of(
            "id", user.getId(),
            "displayName", user.getDisplayName(),
            "profileImageUrl", user.getProfileImageKey() == null ? "" : "/media/" + user.getProfileImageKey()
        );
    }

    private static Map<String, Object> attachment(ListingMessage message) {
        return Map.of(
            "storageKey", message.getAttachmentStorageKey(),
            "contentType", message.getAttachmentContentType(),
            "fileSize", message.getAttachmentFileSize(),
            "mediaType", message.getAttachmentType().name(),
            "url", "/media/" + message.getAttachmentStorageKey()
        );
    }
}
