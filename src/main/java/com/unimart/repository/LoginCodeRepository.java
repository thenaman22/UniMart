package com.unimart.repository;

import com.unimart.domain.LoginCode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginCodeRepository extends JpaRepository<LoginCode, Long> {
    Optional<LoginCode> findFirstByEmailAndCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(String email, String code, Instant now);
}
