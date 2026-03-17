package com.unimart.repository;

import com.unimart.domain.Community;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findBySlug(String slug);
    long countByCreatorId(Long creatorId);
}
