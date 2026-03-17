package com.unimart.api;

import com.unimart.api.dto.MessageDtos.SendMessageRequest;
import com.unimart.api.dto.MessageDtos.MessageAttachmentRequest;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.UserAccount;
import com.unimart.repository.ListingMediaRepository;
import com.unimart.service.ApiException;
import com.unimart.service.MessagingService;
import com.unimart.service.MessagingService.BuyerInboxListing;
import com.unimart.service.MessagingService.ConversationDetail;
import com.unimart.service.MessagingService.SellerInboxListing;
import com.unimart.service.MessagingService.SellerListingDetail;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final MessagingService messagingService;
    private final ListingMediaRepository listingMediaRepository;

    public MessageController(MessagingService messagingService, ListingMediaRepository listingMediaRepository) {
        this.messagingService = messagingService;
        this.listingMediaRepository = listingMediaRepository;
    }

    @PostMapping("/listings/{listingId}/messages")
    public Map<String, Object> createFromListing(
        @PathVariable Long listingId,
        @Valid @RequestBody SendMessageRequest request,
        @CurrentUser AuthContext authContext
    ) {
        UserAccount user = requireAuth(authContext);
        ConversationDetail detail = messagingService.startConversation(
            user,
            listingId,
            request.body(),
            toAttachment(request.attachment())
        );
        return MessageMapper.conversationDetail(detail, user.getId(), previewMedia(detail.conversation().getListing().getId()));
    }

    @GetMapping("/messages/seller")
    public Map<String, Object> sellerInbox(@CurrentUser AuthContext authContext) {
        UserAccount user = requireAuth(authContext);
        return Map.of(
            "listings",
            messagingService.sellerInbox(user).stream()
                .map(this::sellerListing)
                .toList()
        );
    }

    @GetMapping("/messages/seller/listings/{listingId}")
    public Map<String, Object> sellerListingDetail(
        @PathVariable Long listingId,
        @CurrentUser AuthContext authContext
    ) {
        UserAccount user = requireAuth(authContext);
        SellerListingDetail detail = messagingService.sellerListingDetail(user, listingId);
        return Map.of(
            "listing", MessageMapper.listingContext(detail.listing(), previewMedia(detail.listing().getId())),
            "conversations", detail.conversations().stream()
                .map(MessageMapper::sellerConversationSummary)
                .toList()
        );
    }

    @GetMapping("/messages/buyer")
    public Map<String, Object> buyerInbox(@CurrentUser AuthContext authContext) {
        UserAccount user = requireAuth(authContext);
        return Map.of(
            "listings",
            messagingService.buyerInbox(user).stream()
                .map(this::buyerListing)
                .toList()
        );
    }

    @GetMapping("/messages/summary")
    public Map<String, Object> summary(@CurrentUser AuthContext authContext) {
        UserAccount user = requireAuth(authContext);
        MessagingService.InboxSummary summary = messagingService.inboxSummary(user);
        return Map.of(
            "sellerUnreadCount", summary.sellerUnreadCount(),
            "buyerUnreadCount", summary.buyerUnreadCount(),
            "totalUnreadCount", summary.totalUnreadCount()
        );
    }

    @GetMapping("/messages/conversations/{conversationId}")
    public Map<String, Object> conversation(
        @PathVariable Long conversationId,
        @CurrentUser AuthContext authContext
    ) {
        UserAccount user = requireAuth(authContext);
        ConversationDetail detail = messagingService.conversationDetail(user, conversationId);
        return MessageMapper.conversationDetail(detail, user.getId(), previewMedia(detail.conversation().getListing().getId()));
    }

    @PostMapping("/messages/conversations/{conversationId}/messages")
    public Map<String, Object> sendReply(
        @PathVariable Long conversationId,
        @Valid @RequestBody SendMessageRequest request,
        @CurrentUser AuthContext authContext
    ) {
        UserAccount user = requireAuth(authContext);
        ConversationDetail detail = messagingService.sendConversationMessage(
            user,
            conversationId,
            request.body(),
            toAttachment(request.attachment())
        );
        return MessageMapper.conversationDetail(detail, user.getId(), previewMedia(detail.conversation().getListing().getId()));
    }

    @PostMapping("/messages/conversations/{conversationId}/read")
    public Map<String, Object> markRead(
        @PathVariable Long conversationId,
        @CurrentUser AuthContext authContext
    ) {
        UserAccount user = requireAuth(authContext);
        messagingService.markConversationRead(user, conversationId);
        return Map.of("conversationId", conversationId, "read", true);
    }

    private Map<String, Object> sellerListing(SellerInboxListing listing) {
        return MessageMapper.sellerInboxListing(listing, previewMedia(listing.listing().getId()));
    }

    private Map<String, Object> buyerListing(BuyerInboxListing listing) {
        return MessageMapper.buyerInboxListing(listing, previewMedia(listing.conversation().getListing().getId()));
    }

    private ListingMedia previewMedia(Long listingId) {
        return listingMediaRepository.findFirstByListingIdOrderByIdAsc(listingId);
    }

    private MessagingService.MessageAttachment toAttachment(MessageAttachmentRequest request) {
        if (request == null) {
            return null;
        }
        return new MessagingService.MessageAttachment(
            request.storageKey(),
            request.contentType(),
            request.fileSize(),
            request.mediaType()
        );
    }

    private UserAccount requireAuth(AuthContext authContext) {
        if (authContext == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authContext.user();
    }
}
