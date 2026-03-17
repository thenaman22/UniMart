package com.unimart.service;

import com.unimart.domain.Community;
import com.unimart.domain.CommunityDomain;
import com.unimart.domain.CommunityPostingPolicy;
import com.unimart.domain.InviteLink;
import com.unimart.domain.Listing;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.ListingStatus;
import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.MembershipStatus;
import com.unimart.domain.MediaType;
import com.unimart.domain.Report;
import com.unimart.domain.UserAccount;
import com.unimart.repository.CommunityDomainRepository;
import com.unimart.repository.CommunityRepository;
import com.unimart.repository.InviteLinkRepository;
import com.unimart.repository.ListingConversationRepository;
import com.unimart.repository.ListingMediaRepository;
import com.unimart.repository.ListingMessageRepository;
import com.unimart.repository.ListingRepository;
import com.unimart.repository.LoginCodeRepository;
import com.unimart.repository.MembershipRepository;
import com.unimart.repository.ReportRepository;
import com.unimart.repository.UserAccountRepository;
import com.unimart.repository.UserSessionRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedDataService {

    private static final Logger log = LoggerFactory.getLogger(SeedDataService.class);

    private final CommunityRepository communityRepository;
    private final CommunityDomainRepository communityDomainRepository;
    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;
    private final ListingRepository listingRepository;
    private final ListingMediaRepository listingMediaRepository;
    private final ListingConversationRepository listingConversationRepository;
    private final ListingMessageRepository listingMessageRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final ReportRepository reportRepository;
    private final LoginCodeRepository loginCodeRepository;
    private final UserSessionRepository userSessionRepository;
    private final boolean refreshDemoData;
    private final Path storageRoot;

    public SeedDataService(
        CommunityRepository communityRepository,
        CommunityDomainRepository communityDomainRepository,
        UserAccountRepository userAccountRepository,
        MembershipRepository membershipRepository,
        ListingRepository listingRepository,
        ListingMediaRepository listingMediaRepository,
        ListingConversationRepository listingConversationRepository,
        ListingMessageRepository listingMessageRepository,
        InviteLinkRepository inviteLinkRepository,
        ReportRepository reportRepository,
        LoginCodeRepository loginCodeRepository,
        UserSessionRepository userSessionRepository,
        @Value("${app.seed.refresh-demo-data:false}") boolean refreshDemoData,
        @Value("${app.media.storage-path}") String storagePath
    ) {
        this.communityRepository = communityRepository;
        this.communityDomainRepository = communityDomainRepository;
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.listingRepository = listingRepository;
        this.listingMediaRepository = listingMediaRepository;
        this.listingConversationRepository = listingConversationRepository;
        this.listingMessageRepository = listingMessageRepository;
        this.inviteLinkRepository = inviteLinkRepository;
        this.reportRepository = reportRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.userSessionRepository = userSessionRepository;
        this.refreshDemoData = refreshDemoData;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    @Transactional
    void seed() {
        if (refreshDemoData) {
            clearDemoData();
        } else if (communityRepository.count() > 0) {
            return;
        }

        List<UserAccount> campusUsers = new ArrayList<>();
        campusUsers.add(createUser("Campus Admin", "admin@school.edu"));
        campusUsers.add(createUser("Ava Patel", "ava@school.edu"));
        campusUsers.add(createUser("Noah Kim", "noah@school.edu"));

        List<UserAccount> makersUsers = new ArrayList<>();
        makersUsers.add(createUser("Makers Admin", "lead@makers.org"));
        makersUsers.add(createUser("Mia Lopez", "mia@makers.org"));
        makersUsers.add(createUser("Ethan Reed", "ethan@makers.org"));
        makersUsers.add(createUser("Sofia Chen", "sofia@makers.org"));
        makersUsers.add(createUser("Liam Brooks", "liam@makers.org"));
        makersUsers.add(createUser("Grace Hall", "grace@makers.org"));
        makersUsers.add(createUser("Jack Turner", "jack@makers.org"));

        Community campusMarket = createCommunity(
            "campus-market",
            "Campus Market",
            "Private marketplace for students and staff.",
            "school.edu",
            campusUsers.get(0),
            CommunityPostingPolicy.ALL_MEMBERS_CAN_POST
        );
        Community makersExchange = createCommunity(
            "makers-exchange",
            "Makers Exchange",
            "Buy, sell, and swap tools and project gear inside the maker community.",
            "makers.org",
            makersUsers.get(0),
            CommunityPostingPolicy.ALL_MEMBERS_CAN_POST
        );

        addMembership(campusUsers.get(0), campusMarket, MembershipRole.ADMIN, MembershipStatus.ACTIVE);
        addMembership(campusUsers.get(1), campusMarket, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(campusUsers.get(2), campusMarket, MembershipRole.MEMBER, MembershipStatus.ACTIVE);

        addMembership(makersUsers.get(0), makersExchange, MembershipRole.ADMIN, MembershipStatus.ACTIVE);
        addMembership(makersUsers.get(1), makersExchange, MembershipRole.MODERATOR, MembershipStatus.ACTIVE);
        addMembership(makersUsers.get(2), makersExchange, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(makersUsers.get(3), makersExchange, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(makersUsers.get(4), makersExchange, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(makersUsers.get(5), makersExchange, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(makersUsers.get(6), makersExchange, MembershipRole.MEMBER, MembershipStatus.ACTIVE);

        // Cross-community memberships make the seeded demo accounts easier to verify in the UI.
        addMembership(campusUsers.get(0), makersExchange, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(campusUsers.get(1), makersExchange, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(makersUsers.get(0), campusMarket, MembershipRole.MEMBER, MembershipStatus.ACTIVE);
        addMembership(makersUsers.get(1), campusMarket, MembershipRole.MEMBER, MembershipStatus.ACTIVE);

        UserAccount pendingUser = createUser("Pending Student", "pending@school.edu");
        addMembership(pendingUser, campusMarket, MembershipRole.MEMBER, MembershipStatus.PENDING);

        UserAccount revokedUser = createUser("Former Maker", "former@makers.org");
        addMembership(revokedUser, makersExchange, MembershipRole.MEMBER, MembershipStatus.REVOKED);

        createInvite(campusMarket, 25);
        createInvite(makersExchange, 50);

        Listing bike = createListing(
            campusMarket,
            campusUsers.get(1),
            "Blue commuter bike",
            "Lightweight bike with a basket, perfect for getting around campus.",
            new BigDecimal("140.00"),
            "Transport",
            "Good",
            "ImagesData/campus-bike.jpg",
            ListingStatus.ACTIVE
        );
        createListing(
            campusMarket,
            campusUsers.get(2),
            "Mini fridge",
            "Compact dorm fridge with freezer section and adjustable shelves.",
            new BigDecimal("80.00"),
            "Appliances",
            "Very Good",
            "ImagesData/mini-fridge.jpg",
            ListingStatus.ACTIVE
        );
        createListing(
            campusMarket,
            campusUsers.get(0),
            "Engineering textbooks bundle",
            "Three gently used first-year engineering textbooks sold together.",
            new BigDecimal("55.00"),
            "Books",
            "Used",
            "ImagesData/textbooks.jpg",
            ListingStatus.SOLD
        );

        createListing(
            makersExchange,
            makersUsers.get(1),
            "3D printer filament set",
            "Six unopened PLA spools in assorted colors.",
            new BigDecimal("72.00"),
            "Supplies",
            "New",
            "ImagesData/filament.jpg",
            ListingStatus.ACTIVE
        );
        createListing(
            makersExchange,
            makersUsers.get(2),
            "Workbench lamp",
            "Adjustable LED task lamp with clamp mount for a workshop table.",
            new BigDecimal("24.00"),
            "Lighting",
            "Good",
            "ImagesData/lamp.jpg",
            ListingStatus.ACTIVE
        );
        Listing solderingKit = createListing(
            makersExchange,
            makersUsers.get(3),
            "Soldering starter kit",
            "Iron, stand, solder, braid, and a basic multimeter included.",
            new BigDecimal("48.00"),
            "Tools",
            "Good",
            "ImagesData/soldering-kit.jpg",
            ListingStatus.ACTIVE
        );
        createListing(
            makersExchange,
            makersUsers.get(4),
            "Cordless drill",
            "18V drill with charger and two batteries.",
            new BigDecimal("95.00"),
            "Power Tools",
            "Very Good",
            "ImagesData/drill.jpg",
            ListingStatus.ACTIVE
        );
        createListing(
            makersExchange,
            makersUsers.get(5),
            "Laser cutter safety glasses",
            "Protective eyewear for workshop use, barely used.",
            new BigDecimal("18.00"),
            "Safety",
            "Like New",
            "ImagesData/glasses.jpg",
            ListingStatus.ACTIVE
        );
        createListing(
            makersExchange,
            makersUsers.get(6),
            "Arduino starter bundle",
            "Arduino Uno, jumper wires, sensors, and breadboards.",
            new BigDecimal("36.00"),
            "Electronics",
            "Good",
            "ImagesData/arduino.jpg",
            ListingStatus.REMOVED
        );

        createReport(bike, campusUsers.get(2), "Photo looks unclear, please verify the bike condition.");
        createReport(solderingKit, makersUsers.get(1), "Listing mentions solder but not exact brand or safety details.");

        log.info(
            "=== DEMO DATA SEEDED SUCCESSFULLY === communities={}, users={}, memberships={}, listings={}",
            communityRepository.count(),
            userAccountRepository.count(),
            membershipRepository.count(),
            listingRepository.count()
        );
    }

    private void clearDemoData() {
        reportRepository.deleteAllInBatch();
        listingMessageRepository.deleteAllInBatch();
        listingConversationRepository.deleteAllInBatch();
        listingMediaRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        inviteLinkRepository.deleteAllInBatch();
        membershipRepository.deleteAllInBatch();
        communityDomainRepository.deleteAllInBatch();
        userSessionRepository.deleteAllInBatch();
        loginCodeRepository.deleteAllInBatch();
        communityRepository.deleteAllInBatch();
        userAccountRepository.deleteAllInBatch();
        clearStoredMedia();
    }

    private Community createCommunity(
        String slug,
        String name,
        String description,
        String emailDomain,
        UserAccount creator,
        CommunityPostingPolicy postingPolicy
    ) {
        Community community = new Community();
        community.setSlug(slug);
        community.setName(name);
        community.setDescription(description);
        community.setPrivateCommunity(true);
        community.setCreator(creator);
        community.setPostingPolicy(postingPolicy);
        communityRepository.save(community);

        CommunityDomain domain = new CommunityDomain();
        domain.setCommunity(community);
        domain.setEmailDomain(emailDomain);
        communityDomainRepository.save(domain);
        return community;
    }

    private UserAccount createUser(String displayName, String email) {
        UserAccount user = new UserAccount();
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setEmailVerified(true);
        return userAccountRepository.save(user);
    }

    private Membership addMembership(UserAccount user, Community community, MembershipRole role, MembershipStatus status) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setCommunity(community);
        membership.setRole(role);
        membership.setStatus(status);
        return membershipRepository.save(membership);
    }

    private InviteLink createInvite(Community community, int maxUses) {
        InviteLink inviteLink = new InviteLink();
        inviteLink.setCommunity(community);
        inviteLink.setToken(UUID.randomUUID().toString());
        inviteLink.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
        inviteLink.setMaxUses(maxUses);
        inviteLink.setUsedCount(0);
        return inviteLinkRepository.save(inviteLink);
    }

    private Listing createListing(
        Community community,
        UserAccount seller,
        String title,
        String description,
        BigDecimal price,
        String category,
        String itemCondition,
        String imageName,
        ListingStatus status
    ) {
        Listing listing = new Listing();
        listing.setCommunity(community);
        listing.setSeller(seller);
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setPrice(price);
        listing.setCategory(category);
        listing.setItemCondition(itemCondition);
        listing.setStatus(status);
        listingRepository.save(listing);

        ListingMedia media = new ListingMedia();
        media.setListing(listing);
        media.setType(MediaType.IMAGE);
        media.setStorageKey(copySeedImage(imageName));
        media.setContentType("image/jpeg");
        media.setFileSize(120_000L);
        listingMediaRepository.save(media);

        return listing;
    }

    private void createReport(Listing listing, UserAccount reporter, String reason) {
        Report report = new Report();
        report.setListing(listing);
        report.setReporter(reporter);
        report.setReason(reason);
        report.setResolved(false);
        reportRepository.save(report);
    }

    private String copySeedImage(String imagePath) {
        try {
            Files.createDirectories(storageRoot);
            String normalizedImagePath = imagePath.replace('\\', '/');
            Path source = Paths.get(normalizedImagePath).toAbsolutePath().normalize();
            String fileName = source.getFileName().toString();
            Path target = storageRoot.resolve(fileName).normalize();
            if (Files.exists(source)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return fileName;
            }
            log.warn("Seed image not found at {}", source);
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to copy seed image: " + imagePath, exception);
        }
    }

    private void clearStoredMedia() {
        try {
            if (!Files.exists(storageRoot)) {
                return;
            }
            try (var paths = Files.list(storageRoot)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to delete stored media: " + path, exception);
                    }
                });
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to clean media storage", exception);
        }
    }
}
