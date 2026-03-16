package com.unimart.service;

import com.unimart.domain.UserAccount;
import com.unimart.repository.UserAccountRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserAccountRepository userAccountRepository;
    private final MembershipService membershipService;

    public ProfileService(UserAccountRepository userAccountRepository, MembershipService membershipService) {
        this.userAccountRepository = userAccountRepository;
        this.membershipService = membershipService;
    }

    public UserAccount getProfile(Long userId) {
        return userAccountRepository.findById(userId)
            .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserAccount getPublicProfile(UserAccount viewer, Long profileUserId) {
        UserAccount profileUser = getProfile(profileUserId);
        List<Long> sharedCommunityIds = membershipService.sharedActiveCommunityIds(viewer, profileUser);
        if (sharedCommunityIds.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view this seller profile");
        }
        return profileUser;
    }

    @Transactional
    public UserAccount updateProfile(
        Long userId,
        String displayName,
        String bio,
        String phoneNumber,
        String location,
        boolean publicPhoneVisible
    ) {
        UserAccount user = getProfile(userId);
        user.setDisplayName(displayName);
        user.setBio(blankToNull(bio));
        user.setPhoneNumber(blankToNull(phoneNumber));
        user.setLocation(blankToNull(location));
        user.setPublicPhoneVisible(publicPhoneVisible);
        return user;
    }

    @Transactional
    public UserAccount updateProfilePicture(Long userId, String storageKey) {
        UserAccount user = getProfile(userId);
        user.setProfileImageKey(storageKey);
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
