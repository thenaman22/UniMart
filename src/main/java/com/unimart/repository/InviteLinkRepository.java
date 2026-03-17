package com.unimart.repository;

import com.unimart.domain.InviteLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteLinkRepository extends JpaRepository<InviteLink, Long> {
    Optional<InviteLink> findByToken(String token);
    List<InviteLink> findByCommunityId(Long communityId);
}
