package com.unimart.repository;

import com.unimart.domain.UserSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    @Query("""
        select s from UserSession s
        join fetch s.user
        where s.token = :token and s.expiresAt > :now
        """)
    Optional<UserSession> findActiveSessionWithUser(@Param("token") String token, @Param("now") Instant now);
}
