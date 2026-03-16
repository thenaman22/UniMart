package com.unimart.service;

import com.unimart.domain.UserAccount;
import com.unimart.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserAccountRepository userAccountRepository;

    public ProfileService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount getProfile(Long userId) {
        return userAccountRepository.findById(userId)
            .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserAccount updateProfile(Long userId, String displayName, String bio, String phoneNumber, String location) {
        UserAccount user = getProfile(userId);
        user.setDisplayName(displayName);
        user.setBio(blankToNull(bio));
        user.setPhoneNumber(blankToNull(phoneNumber));
        user.setLocation(blankToNull(location));
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
