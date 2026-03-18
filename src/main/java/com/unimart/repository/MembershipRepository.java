package com.unimart.repository;

import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.MembershipStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    interface PendingRequestCountView {
        Long getCommunityId();
        long getPendingRequestCount();
    }

    Optional<Membership> findByUserIdAndCommunityId(Long userId, Long communityId);
    List<Membership> findByUserIdAndStatus(Long userId, MembershipStatus status);
    List<Membership> findByUserIdAndStatusAndRoleIn(Long userId, MembershipStatus status, List<MembershipRole> roles);
    List<Membership> findByCommunityId(Long communityId);
    List<Membership> findByCommunityIdAndStatus(Long communityId, MembershipStatus status);
    List<Membership> findByUserIdInAndStatus(List<Long> userIds, MembershipStatus status);
    List<Membership> findByCommunityIdAndStatusAndRoleIn(Long communityId, MembershipStatus status, List<MembershipRole> roles);

    @Query("""
        select m.community.id as communityId, count(m) as pendingRequestCount
        from Membership m
        where m.community.id in :communityIds and m.status = :status
        group by m.community.id
        """)
    List<PendingRequestCountView> findPendingRequestCountsByCommunityIds(
        @Param("communityIds") List<Long> communityIds,
        @Param("status") MembershipStatus status
    );
}
