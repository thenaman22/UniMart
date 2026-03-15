package com.unimart.repository;

import com.unimart.domain.Membership;
import com.unimart.domain.MembershipStatus;
import com.unimart.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByUserIdAndCommunityId(Long userId, Long communityId);
    List<Membership> findByUserAndStatus(UserAccount user, MembershipStatus status);
    List<Membership> findByCommunityIdAndStatus(Long communityId, MembershipStatus status);
}
