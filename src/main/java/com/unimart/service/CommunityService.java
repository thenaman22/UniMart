package com.unimart.service;

import com.unimart.domain.Community;
import com.unimart.domain.CommunityPostingPolicy;
import com.unimart.domain.Listing;
import com.unimart.domain.ListingConversation;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.ListingMessage;
import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.MembershipStatus;
import com.unimart.domain.UserAccount;
import com.unimart.repository.CommunityDomainRepository;
import com.unimart.repository.CommunityRepository;
import com.unimart.repository.InviteLinkRepository;
import com.unimart.repository.ListingConversationRepository;
import com.unimart.repository.ListingMediaRepository;
import com.unimart.repository.ListingMessageRepository;
import com.unimart.repository.ListingRepository;
import com.unimart.repository.MembershipRepository;
import com.unimart.repository.ReportRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {

    private static final int MAX_CREATED_COMMUNITIES = 5;

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityDomainRepository communityDomainRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final ListingRepository listingRepository;
    private final ListingMediaRepository listingMediaRepository;
    private final ListingConversationRepository listingConversationRepository;
    private final ListingMessageRepository listingMessageRepository;
    private final ReportRepository reportRepository;
    private final MembershipService membershipService;
    private final UploadService uploadService;

    public CommunityService(
        CommunityRepository communityRepository,
        MembershipRepository membershipRepository,
        CommunityDomainRepository communityDomainRepository,
        InviteLinkRepository inviteLinkRepository,
        ListingRepository listingRepository,
        ListingMediaRepository listingMediaRepository,
        ListingConversationRepository listingConversationRepository,
        ListingMessageRepository listingMessageRepository,
        ReportRepository reportRepository,
        MembershipService membershipService,
        UploadService uploadService
    ) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.communityDomainRepository = communityDomainRepository;
        this.inviteLinkRepository = inviteLinkRepository;
        this.listingRepository = listingRepository;
        this.listingMediaRepository = listingMediaRepository;
        this.listingConversationRepository = listingConversationRepository;
        this.listingMessageRepository = listingMessageRepository;
        this.reportRepository = reportRepository;
        this.membershipService = membershipService;
        this.uploadService = uploadService;
    }

    @Transactional
    public Membership createCommunity(UserAccount creator, String name, String description, CommunityPostingPolicy postingPolicy) {
        long ownedCount = communityRepository.countByCreatorId(creator.getId());
        if (ownedCount >= MAX_CREATED_COMMUNITIES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can create up to 5 communities");
        }

        Community community = new Community();
        community.setCreator(creator);
        community.setSlug(generateUniqueSlug(name));
        community.setName(name.trim());
        community.setDescription(description.trim());
        community.setPrivateCommunity(true);
        community.setPostingPolicy(postingPolicy);
        communityRepository.save(community);

        Membership membership = new Membership();
        membership.setUser(creator);
        membership.setCommunity(community);
        membership.setRole(MembershipRole.ADMIN);
        membership.setStatus(MembershipStatus.ACTIVE);
        return membershipRepository.save(membership);
    }

    @Transactional
    public void deleteCommunity(Long userId, Long communityId) {
        Community community = membershipService.requireAdmin(userId, communityId).getCommunity();

        List<Listing> listings = listingRepository.findByCommunityId(communityId);
        List<Long> listingIds = listings.stream().map(Listing::getId).toList();
        List<ListingMedia> listingMedia = listingIds.isEmpty() ? List.of() : listingMediaRepository.findByListingIdIn(listingIds);
        List<ListingConversation> conversations = listingConversationRepository.findByListingCommunityId(communityId);
        List<Long> conversationIds = conversations.stream().map(ListingConversation::getId).toList();
        List<ListingMessage> messages = conversationIds.isEmpty() ? List.of() : listingMessageRepository.findByConversationIdIn(conversationIds);

        List<String> fileKeys = Stream.concat(
            listingMedia.stream().map(ListingMedia::getStorageKey),
            messages.stream().map(ListingMessage::getAttachmentStorageKey)
        )
            .filter(key -> key != null && !key.isBlank())
            .distinct()
            .toList();

        reportRepository.deleteAll(reportRepository.findByListingCommunityId(communityId));
        listingMessageRepository.deleteAll(messages);
        listingConversationRepository.deleteAll(conversations);
        listingMediaRepository.deleteAll(listingMedia);
        listingRepository.deleteAll(listings);
        inviteLinkRepository.deleteAll(inviteLinkRepository.findByCommunityId(communityId));
        communityDomainRepository.deleteAll(communityDomainRepository.findByCommunityId(communityId));
        membershipRepository.deleteAll(membershipRepository.findByCommunityId(communityId));
        communityRepository.delete(community);

        fileKeys.forEach(uploadService::deleteStoredFile);
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = slugify(name);
        String candidate = baseSlug;
        int suffix = 2;
        while (communityRepository.findBySlug(candidate).isPresent()) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "community" : normalized;
    }
}
