package com.unimart.repository;

import com.unimart.domain.ListingMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingMediaRepository extends JpaRepository<ListingMedia, Long> {
    List<ListingMedia> findByListingId(Long listingId);
}
