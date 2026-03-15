package com.unimart.service;

import com.unimart.domain.LoginCode;
import com.unimart.domain.UserAccount;
import com.unimart.domain.UserSession;
import com.unimart.repository.LoginCodeRepository;
import com.unimart.repository.UserAccountRepository;
import com.unimart.repository.UserSessionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final LoginCodeRepository loginCodeRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final int otpExpiryMinutes;
    private final Random random = new Random();

    public AuthService(
        LoginCodeRepository loginCodeRepository,
        UserAccountRepository userAccountRepository,
        UserSessionRepository userSessionRepository,
        @Value("${app.auth.otp-expiry-minutes}") int otpExpiryMinutes
    ) {
        this.loginCodeRepository = loginCodeRepository;
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    @Transactional
    public Map<String, String> requestCode(String email, String displayName) {
        String normalizedEmail = email.toLowerCase();
        String code = String.format("%06d", random.nextInt(1_000_000));

        LoginCode loginCode = new LoginCode();
        loginCode.setEmail(normalizedEmail);
        loginCode.setCode(code);
        loginCode.setExpiresAt(Instant.now().plus(otpExpiryMinutes, ChronoUnit.MINUTES));
        loginCode.setUsed(false);
        loginCodeRepository.save(loginCode);

        userAccountRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            UserAccount user = new UserAccount();
            user.setEmail(normalizedEmail);
            user.setDisplayName(displayName == null || displayName.isBlank() ? normalizedEmail.split("@")[0] : displayName);
            user.setEmailVerified(false);
            return userAccountRepository.save(user);
        });

        return Map.of(
            "message", "Login code generated. Replace this with email delivery in production.",
            "code", code
        );
    }

    @Transactional
    public Map<String, Object> verifyCode(String email, String code) {
        String normalizedEmail = email.toLowerCase();
        LoginCode loginCode = loginCodeRepository
            .findFirstByEmailAndCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(normalizedEmail, code, Instant.now())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired code"));

        loginCode.setUsed(true);

        UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEmailVerified(true);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setToken(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        userSessionRepository.save(session);

        return Map.of(
            "token", session.getToken(),
            "user", Map.of(
                "id", user.getId(),
                "displayName", user.getDisplayName(),
                "email", user.getEmail()
            )
        );
    }
}
