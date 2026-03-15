package com.unimart.repository;

import com.unimart.domain.UserSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByTokenAndExpiresAtAfter(String token, Instant now);
}
