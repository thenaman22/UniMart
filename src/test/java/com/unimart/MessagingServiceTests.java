package com.unimart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.unimart.domain.Community;
import com.unimart.domain.Listing;
import com.unimart.domain.ListingStatus;
import com.unimart.domain.MediaType;
import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.MembershipStatus;
import com.unimart.domain.UserAccount;
import com.unimart.repository.CommunityRepository;
import com.unimart.repository.ListingConversationRepository;
import com.unimart.repository.ListingMessageRepository;
import com.unimart.repository.ListingRepository;
import com.unimart.repository.MembershipRepository;
import com.unimart.repository.UserAccountRepository;
import com.unimart.service.ApiException;
import com.unimart.service.MessagingService;
import com.unimart.service.UploadService;
import java.math.BigDecimal;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MessagingServiceTests {

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingConversationRepository listingConversationRepository;

    @Autowired
    private ListingMessageRepository listingMessageRepository;

    @Autowired
    private UploadService uploadService;

    @Test
    void reusesConversationForRepeatBuyerMessagesOnSameListing() {
        UserAccount seller = saveUser("seller@uni.edu", "Seller");
        UserAccount buyer = saveUser("buyer@uni.edu", "Buyer");
        Community community = saveCommunity();
        addMembership(seller, community);
        addMembership(buyer, community);
        Listing listing = saveListing(seller, community);

        messagingService.startConversation(buyer, listing.getId(), "Hi, is this still available?", null);
        messagingService.startConversation(buyer, listing.getId(), "Can you share pickup details?", null);

        var conversation = listingConversationRepository.findByListingIdAndBuyerId(listing.getId(), buyer.getId()).orElseThrow();
        var messages = listingMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        assertThat(messages).hasSize(2);
        assertThat(conversation.getSellerUnreadCount()).isEqualTo(2);
        assertThat(conversation.getBuyerUnreadCount()).isZero();
        assertThat(conversation.getBuyer().getId()).isEqualTo(buyer.getId());
        assertThat(conversation.getSeller().getId()).isEqualTo(seller.getId());
    }

    @Test
    void blocksNewMessagesWhenListingIsInactiveButKeepsConversationReadable() {
        UserAccount seller = saveUser("seller2@uni.edu", "Seller Two");
        UserAccount buyer = saveUser("buyer2@uni.edu", "Buyer Two");
        Community community = saveCommunity();
        addMembership(seller, community);
        addMembership(buyer, community);
        Listing listing = saveListing(seller, community);

        var detail = messagingService.startConversation(buyer, listing.getId(), "Interested in your listing.", null);
        listing.setStatus(ListingStatus.SOLD);

        assertThatThrownBy(() -> messagingService.sendConversationMessage(seller, detail.conversation().getId(), "It sold this morning.", null))
            .isInstanceOf(ApiException.class)
            .hasMessage("This listing is no longer accepting messages");

        var conversation = messagingService.conversationDetail(buyer, detail.conversation().getId());
        assertThat(conversation.readOnly()).isTrue();
        assertThat(conversation.messages()).hasSize(1);
    }

    @Test
    void supportsImageMessagesWithoutText() {
        UserAccount seller = saveUser("seller3@uni.edu", "Seller Three");
        UserAccount buyer = saveUser("buyer3@uni.edu", "Buyer Three");
        Community community = saveCommunity();
        addMembership(seller, community);
        addMembership(buyer, community);
        Listing listing = saveListing(seller, community);

        MockMultipartFile file = new MockMultipartFile("file", "bike.jpg", "image/jpeg", new byte[] {1, 2, 3, 4});
        var upload = uploadService.storeFile(file);

        var detail = messagingService.startConversation(
            buyer,
            listing.getId(),
            "",
            new MessagingService.MessageAttachment(
                (String) upload.get("storageKey"),
                (String) upload.get("contentType"),
                ((Number) upload.get("fileSize")).longValue(),
                MediaType.IMAGE
            )
        );

        assertThat(detail.messages()).hasSize(1);
        assertThat(detail.messages().get(0).getBody()).isEmpty();
        assertThat(detail.messages().get(0).getAttachmentType()).isEqualTo(MediaType.IMAGE);
        assertThat(detail.messages().get(0).getAttachmentStorageKey()).isNotBlank();
    }

    private UserAccount saveUser(String email, String displayName) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setEmailVerified(true);
        return userAccountRepository.save(user);
    }

    private Community saveCommunity() {
        Community community = new Community();
        community.setSlug("campus-" + System.nanoTime());
        community.setName("Campus Market");
        community.setDescription("Private campus exchange");
        community.setPrivateCommunity(true);
        return communityRepository.save(community);
    }

    private void addMembership(UserAccount user, Community community) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setCommunity(community);
        membership.setRole(MembershipRole.MEMBER);
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);
    }

    private Listing saveListing(UserAccount seller, Community community) {
        Listing listing = new Listing();
        listing.setSeller(seller);
        listing.setCommunity(community);
        listing.setTitle("Desk lamp");
        listing.setDescription("Warm light desk lamp");
        listing.setPrice(new BigDecimal("20.00"));
        listing.setCategory("Home");
        listing.setItemCondition("Used");
        listing.setStatus(ListingStatus.ACTIVE);
        return listingRepository.save(listing);
    }
}
