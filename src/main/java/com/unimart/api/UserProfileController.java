package com.unimart.api;

import com.unimart.domain.Listing;
import com.unimart.domain.UserAccount;
import com.unimart.service.ApiException;
import com.unimart.service.ListingService;
import com.unimart.service.ProfileService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserProfileController {

    private final ProfileService profileService;
    private final ListingService listingService;

    public UserProfileController(ProfileService profileService, ListingService listingService) {
        this.profileService = profileService;
        this.listingService = listingService;
    }

    @GetMapping("/{userId}/profile")
    public Map<String, Object> getPublicProfile(@PathVariable Long userId, @CurrentUser AuthContext authContext) {
        UserAccount viewer = requireAuth(authContext);
        UserAccount profileUser = profileService.getPublicProfile(viewer, userId);
        List<Listing> listings = listingService.activeListingsForSellerProfile(viewer, profileUser);
        return Map.of(
            "profile", Mapper.publicProfile(profileUser),
            "activeListings", listings.stream()
                .map(listing -> Mapper.listingSummary(listing, listingService.mediaForListing(listing.getId(), viewer)))
                .toList()
        );
    }

    private UserAccount requireAuth(AuthContext authContext) {
        if (authContext == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authContext.user();
    }
}
