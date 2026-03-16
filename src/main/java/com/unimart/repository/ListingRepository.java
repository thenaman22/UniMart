package com.unimart.repository;

import com.unimart.domain.Listing;
import com.unimart.domain.ListingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByCommunityIdInOrderByCreatedAtDesc(List<Long> communityIds);
    List<Listing> findByCommunityIdInAndStatusOrderByCreatedAtDesc(List<Long> communityIds, ListingStatus status);
}
