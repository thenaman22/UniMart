package com.unimart.service;

import com.unimart.domain.Listing;
import com.unimart.domain.ListingMedia;
import com.unimart.domain.ListingStatus;
import com.unimart.domain.Membership;
import com.unimart.domain.UserAccount;
import com.unimart.repository.ListingMediaRepository;
import com.unimart.repository.ListingRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingMediaRepository listingMediaRepository;
    private final MembershipService membershipService;

    public ListingService(
        ListingRepository listingRepository,
        ListingMediaRepository listingMediaRepository,
        MembershipService membershipService
    ) {
        this.listingRepository = listingRepository;
        this.listingMediaRepository = listingMediaRepository;
        this.membershipService = membershipService;
    }

    @Transactional
    public Listing createListing(
        UserAccount user,
        Long communityId,
        String title,
        String description,
        BigDecimal price,
        String category,
        String itemCondition,
        List<ListingMedia> media
    ) {
        Membership membership = membershipService.requireActiveMembership(user.getId(), communityId);
        Listing listing = new Listing();
        listing.setCommunity(membership.getCommunity());
        listing.setSeller(user);
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setPrice(price);
        listing.setCategory(category);
        listing.setItemCondition(itemCondition);
        listingRepository.save(listing);

        for (ListingMedia listingMedia : media) {
            listingMedia.setListing(listing);
            listingMediaRepository.save(listingMedia);
        }
        return listing;
    }

    public Listing getAccessibleListing(Long listingId, UserAccount user) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Listing not found"));
        membershipService.requireActiveMembership(user.getId(), listing.getCommunity().getId());
        return listing;
    }

    public List<Listing> searchListings(
        UserAccount user,
        String query,
        String category,
        String itemCondition,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ListingStatus status
    ) {
        List<Long> communityIds = membershipService.activeMemberships(user).stream()
            .map(membership -> membership.getCommunity().getId())
            .toList();
        if (communityIds.isEmpty()) {
            return List.of();
        }
        return listingRepository.searchAccessibleListings(communityIds, status, query, category, itemCondition, minPrice, maxPrice);
    }

    @Transactional
    public Listing updateStatus(Long listingId, UserAccount user, ListingStatus status) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Listing not found"));
        boolean moderator = false;
        try {
            membershipService.requireModerator(user.getId(), listing.getCommunity().getId());
            moderator = true;
        } catch (ApiException ignored) {
            membershipService.requireActiveMembership(user.getId(), listing.getCommunity().getId());
        }
        if (!moderator && !listing.getSeller().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot update this listing");
        }
        listing.setStatus(status);
        return listing;
    }

    @Transactional
    public Listing updateListing(
        Long listingId,
        UserAccount user,
        String title,
        String description,
        BigDecimal price,
        String category,
        String itemCondition
    ) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Listing not found"));
        membershipService.requireActiveMembership(user.getId(), listing.getCommunity().getId());
        if (!listing.getSeller().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the seller can edit this listing");
        }
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setPrice(price);
        listing.setCategory(category);
        listing.setItemCondition(itemCondition);
        return listing;
    }

    public List<ListingMedia> mediaForListing(Long listingId, UserAccount user) {
        getAccessibleListing(listingId, user);
        return listingMediaRepository.findByListingId(listingId);
    }
}
