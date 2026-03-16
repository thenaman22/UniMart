package com.unimart.repository;

import com.unimart.domain.Membership;
import com.unimart.domain.MembershipStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByUserIdAndCommunityId(Long userId, Long communityId);
    List<Membership> findByUserIdAndStatus(Long userId, MembershipStatus status);
    List<Membership> findByCommunityIdAndStatus(Long communityId, MembershipStatus status);
    List<Membership> findByUserIdInAndStatus(List<Long> userIds, MembershipStatus status);
}
