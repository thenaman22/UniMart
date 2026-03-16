package com.unimart.service;

import com.unimart.domain.Listing;
import com.unimart.domain.ListingConversation;
import com.unimart.domain.ListingMessage;
import com.unimart.domain.ListingStatus;
import com.unimart.domain.UserAccount;
import com.unimart.repository.ListingConversationRepository;
import com.unimart.repository.ListingMessageRepository;
import com.unimart.repository.ListingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingService {

    private final ListingRepository listingRepository;
    private final ListingConversationRepository listingConversationRepository;
    private final ListingMessageRepository listingMessageRepository;
    private final MembershipService membershipService;

    public MessagingService(
        ListingRepository listingRepository,
        ListingConversationRepository listingConversationRepository,
        ListingMessageRepository listingMessageRepository,
        MembershipService membershipService
    ) {
        this.listingRepository = listingRepository;
        this.listingConversationRepository = listingConversationRepository;
        this.listingMessageRepository = listingMessageRepository;
        this.membershipService = membershipService;
    }

    @Transactional
    public ConversationDetail startConversation(UserAccount sender, Long listingId, String body) {
        Listing listing = requireAccessibleListing(listingId, sender);
        if (listing.getSeller().getId().equals(sender.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot message your own listing");
        }
        ensureListingAcceptsMessages(listing);

        ListingConversation conversation = listingConversationRepository.findByListingIdAndBuyerId(listingId, sender.getId())
            .orElseGet(() -> createConversation(listing, sender));

        appendMessage(conversation, sender, body);
        return conversationDetail(sender, conversation.getId());
    }

    @Transactional
    public ConversationDetail sendConversationMessage(UserAccount sender, Long conversationId, String body) {
        ListingConversation conversation = requireParticipant(conversationId, sender);
        ensureListingAcceptsMessages(conversation.getListing());
        appendMessage(conversation, sender, body);
        return conversationDetail(sender, conversationId);
    }

    public List<SellerInboxListing> sellerInbox(UserAccount seller) {
        List<Long> communityIds = membershipService.activeCommunityIds(seller);
        if (communityIds.isEmpty()) {
            return List.of();
        }
        List<ListingConversation> conversations = listingConversationRepository
            .findBySellerIdAndListingCommunityIdInOrderByLastMessageAtDesc(seller.getId(), communityIds);

        Map<Long, SellerInboxListing> grouped = new LinkedHashMap<>();
        for (ListingConversation conversation : conversations) {
            Listing listing = conversation.getListing();
            SellerInboxListing current = grouped.get(listing.getId());
            if (current == null) {
                grouped.put(
                    listing.getId(),
                    new SellerInboxListing(listing, conversation.getLastMessageAt(), conversation.getSellerUnreadCount(), 1)
                );
                continue;
            }
            grouped.put(
                listing.getId(),
                new SellerInboxListing(
                    listing,
                    current.lastMessageAt(),
                    current.unreadCount() + conversation.getSellerUnreadCount(),
                    current.conversationCount() + 1
                )
            );
        }
        return List.copyOf(grouped.values());
    }

    public SellerListingDetail sellerListingDetail(UserAccount seller, Long listingId) {
        Listing listing = requireOwnedListing(listingId, seller);
        List<ConversationPreview> conversations = listingConversationRepository.findByListingIdOrderByLastMessageAtDesc(listingId).stream()
            .map(conversation -> new ConversationPreview(
                conversation,
                conversation.getSellerUnreadCount(),
                lastMessagePreview(conversation.getId())
            ))
            .toList();
        return new SellerListingDetail(listing, conversations);
    }

    public List<BuyerInboxListing> buyerInbox(UserAccount buyer) {
        List<Long> communityIds = membershipService.activeCommunityIds(buyer);
        if (communityIds.isEmpty()) {
            return List.of();
        }
        return listingConversationRepository
            .findByBuyerIdAndListingCommunityIdInOrderByLastMessageAtDesc(buyer.getId(), communityIds)
            .stream()
            .map(conversation -> new BuyerInboxListing(conversation, lastMessagePreview(conversation.getId())))
            .toList();
    }

    public ConversationDetail conversationDetail(UserAccount user, Long conversationId) {
        ListingConversation conversation = requireParticipant(conversationId, user);
        List<ListingMessage> messages = listingMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        boolean readOnly = conversation.getListing().getStatus() != ListingStatus.ACTIVE;
        return new ConversationDetail(conversation, messages, readOnly);
    }

    public InboxSummary inboxSummary(UserAccount user) {
        int sellerUnreadCount = sellerInbox(user).stream()
            .mapToInt(SellerInboxListing::unreadCount)
            .sum();
        int buyerUnreadCount = buyerInbox(user).stream()
            .mapToInt(item -> item.conversation().getBuyerUnreadCount())
            .sum();
        return new InboxSummary(sellerUnreadCount, buyerUnreadCount);
    }

    @Transactional
    public void markConversationRead(UserAccount user, Long conversationId) {
        ListingConversation conversation = requireParticipant(conversationId, user);
        if (conversation.getSeller().getId().equals(user.getId())) {
            conversation.setSellerUnreadCount(0);
            return;
        }
        conversation.setBuyerUnreadCount(0);
    }

    private Listing requireAccessibleListing(Long listingId, UserAccount user) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Listing not found"));
        membershipService.requireActiveMembership(user.getId(), listing.getCommunity().getId());
        return listing;
    }

    private Listing requireOwnedListing(Long listingId, UserAccount seller) {
        Listing listing = requireAccessibleListing(listingId, seller);
        if (!listing.getSeller().getId().equals(seller.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view conversations for this listing");
        }
        return listing;
    }

    private ListingConversation requireParticipant(Long conversationId, UserAccount user) {
        ListingConversation conversation = listingConversationRepository.findById(conversationId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
        membershipService.requireActiveMembership(user.getId(), conversation.getListing().getCommunity().getId());
        if (!conversation.getSeller().getId().equals(user.getId()) && !conversation.getBuyer().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this conversation");
        }
        return conversation;
    }

    private ListingConversation createConversation(Listing listing, UserAccount buyer) {
        ListingConversation conversation = new ListingConversation();
        conversation.setListing(listing);
        conversation.setSeller(listing.getSeller());
        conversation.setBuyer(buyer);
        conversation.setLastMessageAt(Instant.now());
        conversation.setSellerUnreadCount(0);
        conversation.setBuyerUnreadCount(0);
        return listingConversationRepository.save(conversation);
    }

    private void ensureListingAcceptsMessages(Listing listing) {
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This listing is no longer accepting messages");
        }
    }

    private void appendMessage(ListingConversation conversation, UserAccount sender, String body) {
        ListingMessage message = new ListingMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setBody(body.trim());
        listingMessageRepository.save(message);

        conversation.setLastMessageAt(Instant.now());
        if (conversation.getSeller().getId().equals(sender.getId())) {
            conversation.setBuyerUnreadCount(conversation.getBuyerUnreadCount() + 1);
            return;
        }
        conversation.setSellerUnreadCount(conversation.getSellerUnreadCount() + 1);
    }

    private String lastMessagePreview(Long conversationId) {
        return listingMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
            .map(ListingMessage::getBody)
            .map(body -> body.length() > 120 ? body.substring(0, 117) + "..." : body)
            .orElse("");
    }

    public record SellerInboxListing(
        Listing listing,
        Instant lastMessageAt,
        int unreadCount,
        long conversationCount
    ) {}

    public record SellerListingDetail(Listing listing, List<ConversationPreview> conversations) {}

    public record BuyerInboxListing(ListingConversation conversation, String lastMessagePreview) {}

    public record ConversationPreview(
        ListingConversation conversation,
        int unreadCount,
        String lastMessagePreview
    ) {}

    public record ConversationDetail(
        ListingConversation conversation,
        List<ListingMessage> messages,
        boolean readOnly
    ) {}

    public record InboxSummary(int sellerUnreadCount, int buyerUnreadCount) {
        public int totalUnreadCount() {
            return sellerUnreadCount + buyerUnreadCount;
        }
    }
}
