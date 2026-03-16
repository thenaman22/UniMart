package com.unimart.api;

import com.unimart.api.dto.ProfileDtos.UpdateProfileRequest;
import com.unimart.domain.Listing;
import com.unimart.domain.UserAccount;
import com.unimart.service.ApiException;
import com.unimart.service.ListingService;
import com.unimart.service.ProfileService;
import com.unimart.service.UploadService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ListingService listingService;
    private final UploadService uploadService;

    public ProfileController(ProfileService profileService, ListingService listingService, UploadService uploadService) {
        this.profileService = profileService;
        this.listingService = listingService;
        this.uploadService = uploadService;
    }

    @GetMapping
    public Map<String, Object> getProfile(@CurrentUser AuthContext authContext) {
        UserAccount user = requireAuth(authContext);
        List<Listing> listings = listingService.listingsForOwner(user);
        return Map.of(
            "profile", Mapper.selfProfile(profileService.getProfile(user.getId())),
            "myListings", listings.stream()
                .map(listing -> Mapper.listingSummary(listing, listingService.mediaForListing(listing.getId(), user)))
                .toList()
        );
    }

    @PatchMapping
    public Map<String, Object> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request,
        @CurrentUser AuthContext authContext
    ) {
        UserAccount user = requireAuth(authContext);
        return Mapper.selfProfile(profileService.updateProfile(
            user.getId(),
            request.displayName(),
            request.bio(),
            request.phoneNumber(),
            request.location(),
            request.publicPhoneVisible()
        ));
    }

    @PostMapping("/picture")
    public Map<String, Object> updateProfilePicture(
        @RequestParam("file") MultipartFile file,
        @CurrentUser AuthContext authContext
    ) {
        UserAccount user = requireAuth(authContext);
        Map<String, Object> upload = uploadService.storeFile(file);
        return Mapper.selfProfile(profileService.updateProfilePicture(user.getId(), (String) upload.get("storageKey")));
    }

    private UserAccount requireAuth(AuthContext authContext) {
        if (authContext == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authContext.user();
    }

}
