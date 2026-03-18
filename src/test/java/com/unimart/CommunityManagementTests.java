package com.unimart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.unimart.api.AuthContext;
import com.unimart.api.ModerationController;
import com.unimart.domain.Community;
import com.unimart.domain.CommunityDomain;
import com.unimart.domain.CommunityPostingPolicy;
import com.unimart.domain.Listing;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.MembershipStatus;
import com.unimart.domain.Report;
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
import com.unimart.repository.UserAccountRepository;
import com.unimart.service.ApiException;
import com.unimart.service.CommunityService;
import com.unimart.service.ListingService;
import com.unimart.service.MessagingService;
import com.unimart.service.MembershipService;
import com.unimart.service.UploadService;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CommunityManagementTests {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private ListingService listingService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private ModerationController moderationController;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CommunityDomainRepository communityDomainRepository;

    @Autowired
    private InviteLinkRepository inviteLinkRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingMediaRepository listingMediaRepository;

    @Autowired
    private ListingConversationRepository listingConversationRepository;

    @Autowired
    private ListingMessageRepository listingMessageRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void createsCommunityWithCreatorAsAdmin() {
        UserAccount creator = saveUser("creator-one");

        Membership membership = communityService.createCommunity(
            creator,
            "Garage Sale",
            "Neighborhood pickup items.",
            CommunityPostingPolicy.CREATOR_ONLY
        );

        assertThat(membership.getRole()).isEqualTo(MembershipRole.ADMIN);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getCommunity().getCreator().getId()).isEqualTo(creator.getId());
        assertThat(membership.getCommunity().getPostingPolicy()).isEqualTo(CommunityPostingPolicy.CREATOR_ONLY);
    }

    @Test
    void rejectsCreatingMoreThanFiveCommunitiesForOneCreator() {
        UserAccount creator = saveUser("creator-limit");

        for (int index = 0; index < 5; index++) {
            communityService.createCommunity(
                creator,
                "Community " + index,
                "Description " + index,
                CommunityPostingPolicy.ALL_MEMBERS_CAN_POST
            );
        }

        assertThatThrownBy(() -> communityService.createCommunity(
            creator,
            "Community 6",
            "Overflow",
            CommunityPostingPolicy.ALL_MEMBERS_CAN_POST
        ))
            .isInstanceOf(ApiException.class)
            .hasMessage("You can create up to 5 communities");
    }

    @Test
    void enforcesPostingPolicyByRole() {
        UserAccount creator = saveUser("posting-creator");
        UserAccount viewer = saveUser("posting-viewer");
        UserAccount seller = saveUser("posting-seller");

        Community allMembers = createCommunity(creator, "All", CommunityPostingPolicy.ALL_MEMBERS_CAN_POST);
        Community sellerOnly = createCommunity(creator, "Seller", CommunityPostingPolicy.APPROVED_SELLERS_ONLY);
        Community creatorOnly = createCommunity(creator, "Creator", CommunityPostingPolicy.CREATOR_ONLY);

        addMembership(viewer, allMembers, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(viewer, sellerOnly, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(viewer, creatorOnly, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(seller, sellerOnly, MembershipRole.SELLER, MembershipStatus.ACTIVE);
        addMembership(seller, creatorOnly, MembershipRole.SELLER, MembershipStatus.ACTIVE);

        Listing memberListing = createListing(viewer, allMembers);
        Listing sellerListing = createListing(seller, sellerOnly);
        Listing creatorListing = createListing(creator, creatorOnly);

        assertThat(memberListing.getId()).isNotNull();
        assertThat(sellerListing.getId()).isNotNull();
        assertThat(creatorListing.getId()).isNotNull();

        assertThatThrownBy(() -> createListing(viewer, sellerOnly))
            .isInstanceOf(ApiException.class)
            .hasMessage("You cannot post listings in this community");

        assertThatThrownBy(() -> createListing(seller, creatorOnly))
            .isInstanceOf(ApiException.class)
            .hasMessage("You cannot post listings in this community");
    }

    @Test
    void onlyAdminsCanUpdateSellerAndModeratorRoles() {
        UserAccount creator = saveUser("role-admin");
        UserAccount moderator = saveUser("role-moderator");
        UserAccount member = saveUser("role-member");
        Community community = createCommunity(creator, "Roles", CommunityPostingPolicy.APPROVED_SELLERS_ONLY);

        addMembership(moderator, community, MembershipRole.MODERATOR, MembershipStatus.ACTIVE);
        Membership memberMembership = addMembership(member, community, MembershipRole.MEMBER, MembershipStatus.ACTIVE);

        Membership updated = membershipService.updateRole(creator.getId(), community.getId(), memberMembership.getId(), MembershipRole.SELLER);
        assertThat(updated.getRole()).isEqualTo(MembershipRole.SELLER);

        assertThatThrownBy(() -> membershipService.updateRole(
            moderator.getId(),
            community.getId(),
            memberMembership.getId(),
            MembershipRole.MODERATOR
        ))
            .isInstanceOf(ApiException.class)
            .hasMessage("Admin access required");
    }

    @Test
    void rejectsSellerRoleForCommunitiesWithoutSellerApproval() {
        UserAccount creator = saveUser("role-policy-admin");
        UserAccount member = saveUser("role-policy-member");
        Community openCommunity = createCommunity(creator, "Open roles", CommunityPostingPolicy.ALL_MEMBERS_CAN_POST);
        Community creatorOnlyCommunity = createCommunity(creator, "Creator roles", CommunityPostingPolicy.CREATOR_ONLY);

        Membership openMembership = addMembership(member, openCommunity, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        Membership creatorOnlyMembership = addMembership(
            member,
            creatorOnlyCommunity,
            MembershipRole.MEMBER,
            MembershipStatus.ACTIVE
        );

        assertThatThrownBy(() -> membershipService.updateRole(
            creator.getId(),
            openCommunity.getId(),
            openMembership.getId(),
            MembershipRole.SELLER
        ))
            .isInstanceOf(ApiException.class)
            .hasMessage("Seller role is only available in communities that require approved sellers");

        assertThatThrownBy(() -> membershipService.updateRole(
            creator.getId(),
            creatorOnlyCommunity.getId(),
            creatorOnlyMembership.getId(),
            MembershipRole.SELLER
        ))
            .isInstanceOf(ApiException.class)
            .hasMessage("Seller role is only available in communities that require approved sellers");
    }

    @Test
    void autoAssignsMembershipsByAllowedDomain() {
        UserAccount creator = saveUser("domain-admin");
        UserAccount newUser = saveUser("domain-member", false, "student@campus.edu");
        Community community = createCommunity(creator, "Campus", CommunityPostingPolicy.ALL_MEMBERS_CAN_POST);

        CommunityDomain domain = new CommunityDomain();
        domain.setCommunity(community);
        domain.setEmailDomain("campus.edu");
        communityDomainRepository.save(domain);

        List<Membership> memberships = membershipService.autoAssignMembershipsForUser(newUser);

        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).getCommunity().getId()).isEqualTo(community.getId());
        assertThat(memberships.get(0).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void hardDeleteRemovesCommunityDataAndMedia() throws Exception {
        UserAccount creator = saveUser("delete-admin");
        UserAccount buyer = saveUser("delete-buyer");
        Community community = createCommunity(creator, "Delete Me", CommunityPostingPolicy.ALL_MEMBERS_CAN_POST);
        addMembership(buyer, community, MembershipRole.MEMBER, MembershipStatus.ACTIVE);

        membershipService.addDomain(creator.getId(), community.getId(), "delete.edu");
        membershipService.createInvite(creator.getId(), community.getId(), 5);

        MockMultipartFile file = new MockMultipartFile("file", "delete.jpg", "image/jpeg", new byte[] {1, 2, 3});
        var upload = uploadService.storeFile(file);
        String storageKey = (String) upload.get("storageKey");

        Listing listing = listingService.createListing(
            creator,
            community.getId(),
            "To delete",
            "Cleanup listing",
            new BigDecimal("12.00"),
            "Garage",
            "Used",
            List.of(media(storageKey, (String) upload.get("contentType"), ((Number) upload.get("fileSize")).longValue()))
        );

        Report report = new Report();
        report.setListing(listing);
        report.setReporter(buyer);
        report.setReason("Test report");
        report.setResolved(false);
        reportRepository.save(report);

        membershipService.joinByInvite(buyer, inviteLinkRepository.findByCommunityId(community.getId()).get(0).getToken());
        Long conversationId = messagingService.startConversation(buyer, listing.getId(), "Interested", null)
            .conversation()
            .getId();

        communityService.deleteCommunity(creator.getId(), community.getId());

        assertThat(communityRepository.findById(community.getId())).isEmpty();
        assertThat(membershipRepository.findByCommunityId(community.getId())).isEmpty();
        assertThat(communityDomainRepository.findByCommunityId(community.getId())).isEmpty();
        assertThat(inviteLinkRepository.findByCommunityId(community.getId())).isEmpty();
        assertThat(listingRepository.findByCommunityId(community.getId())).isEmpty();
        assertThat(reportRepository.findByListingCommunityId(community.getId())).isEmpty();
        assertThat(listingConversationRepository.findByListingCommunityId(community.getId())).isEmpty();
        assertThat(listingMessageRepository.findByConversationIdIn(List.of(conversationId))).isEmpty();
        assertThat(Files.exists(uploadService.resolvePath(storageKey))).isFalse();
    }

    @Test
    void moderationSummaryCountsOnlyManageablePendingRequests() {
        UserAccount moderator = saveUser("summary-moderator");
        UserAccount pendingOne = saveUser("summary-pending-one");
        UserAccount pendingTwo = saveUser("summary-pending-two");
        UserAccount pendingThree = saveUser("summary-pending-three");
        UserAccount excludedPending = saveUser("summary-excluded");
        UserAccount reporter = saveUser("summary-reporter");

        Community firstManaged = createCommunity(saveUser("summary-admin-one"), "Managed A", CommunityPostingPolicy.ALL_MEMBERS_CAN_POST);
        Community secondManaged = createCommunity(saveUser("summary-admin-two"), "Managed B", CommunityPostingPolicy.ALL_MEMBERS_CAN_POST);
        Community managedWithoutRequests = createCommunity(
            saveUser("summary-admin-three"),
            "Managed C",
            CommunityPostingPolicy.ALL_MEMBERS_CAN_POST
        );
        Community unmanageable = createCommunity(saveUser("summary-admin-four"), "Unmanaged", CommunityPostingPolicy.ALL_MEMBERS_CAN_POST);

        addMembership(moderator, firstManaged, MembershipRole.MODERATOR, MembershipStatus.ACTIVE);
        addMembership(moderator, secondManaged, MembershipRole.ADMIN, MembershipStatus.ACTIVE);
        addMembership(moderator, managedWithoutRequests, MembershipRole.MODERATOR, MembershipStatus.ACTIVE);
        addMembership(moderator, unmanageable, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(reporter, firstManaged, MembershipRole.MEMBER, MembershipStatus.ACTIVE);

        addMembership(pendingOne, firstManaged, MembershipRole.MEMBER, MembershipStatus.PENDING);
        addMembership(pendingTwo, firstManaged, MembershipRole.MEMBER, MembershipStatus.PENDING);
        addMembership(pendingThree, secondManaged, MembershipRole.MEMBER, MembershipStatus.PENDING);
        addMembership(excludedPending, unmanageable, MembershipRole.MEMBER, MembershipStatus.PENDING);

        Listing reportListing = createListing(reporter, firstManaged);
        Report report = new Report();
        report.setListing(reportListing);
        report.setReporter(reporter);
        report.setReason("Still pending report");
        report.setResolved(false);
        reportRepository.save(report);

        Map<String, Object> summary = moderationController.summary(new AuthContext(moderator));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> communities = (List<Map<String, Object>>) summary.get("communities");

        assertThat(summary.get("pendingRequestCount")).isEqualTo(3);
        assertThat(communities).containsExactly(
            Map.of("communityId", firstManaged.getId(), "pendingRequestCount", 2),
            Map.of("communityId", secondManaged.getId(), "pendingRequestCount", 1),
            Map.of("communityId", managedWithoutRequests.getId(), "pendingRequestCount", 0)
        );
    }

    @Test
    void moderationSummaryIsEmptyWithoutManageableCommunities() {
        UserAccount viewer = saveUser("summary-viewer");
        Community community = createCommunity(saveUser("summary-owner"), "Viewer only", CommunityPostingPolicy.ALL_MEMBERS_CAN_POST);

        addMembership(viewer, community, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(saveUser("summary-other-pending"), community, MembershipRole.MEMBER, MembershipStatus.PENDING);

        Map<String, Object> summary = moderationController.summary(new AuthContext(viewer));

        assertThat(summary.get("pendingRequestCount")).isEqualTo(0);
        assertThat(summary.get("communities")).isEqualTo(List.of());
    }

    private UserAccount saveUser(String key) {
        return saveUser(key, true, key + "@example.com");
    }

    private UserAccount saveUser(String key, boolean verified, String email) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setDisplayName(key);
        user.setEmailVerified(verified);
        return userAccountRepository.save(user);
    }

    private Community createCommunity(UserAccount creator, String name, CommunityPostingPolicy postingPolicy) {
        return communityService.createCommunity(creator, name, name + " description", postingPolicy).getCommunity();
    }

    private Membership addMembership(UserAccount user, Community community, MembershipRole role, MembershipStatus status) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setCommunity(community);
        membership.setRole(role);
        membership.setStatus(status);
        return membershipRepository.save(membership);
    }

    private Listing createListing(UserAccount seller, Community community) {
        return listingService.createListing(
            seller,
            community.getId(),
            "Desk lamp",
            "Warm light",
            new BigDecimal("20.00"),
            "Home",
            "Used",
            List.of()
        );
    }

    private ListingMedia media(String storageKey, String contentType, long fileSize) {
        ListingMedia media = new ListingMedia();
        media.setStorageKey(storageKey);
        media.setContentType(contentType);
        media.setFileSize(fileSize);
        media.setType(com.unimart.domain.MediaType.IMAGE);
        return media;
    }
}
