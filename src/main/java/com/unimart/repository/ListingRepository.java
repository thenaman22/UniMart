package com.unimart.repository;

import com.unimart.domain.Listing;
import com.unimart.domain.ListingStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    @Query("""
        select l from Listing l
        where l.community.id in :communityIds
          and (:status is null or l.status = :status)
          and (:query is null or lower(l.title) like lower(concat('%', :query, '%'))
               or lower(l.description) like lower(concat('%', :query, '%'))
               or lower(l.category) like lower(concat('%', :query, '%')))
          and (:category is null or lower(l.category) = lower(:category))
          and (:itemCondition is null or lower(l.itemCondition) = lower(:itemCondition))
          and (:minPrice is null or l.price >= :minPrice)
          and (:maxPrice is null or l.price <= :maxPrice)
        order by l.createdAt desc
        """)
    List<Listing> searchAccessibleListings(
        @Param("communityIds") List<Long> communityIds,
        @Param("status") ListingStatus status,
        @Param("query") String query,
        @Param("category") String category,
        @Param("itemCondition") String itemCondition,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice
    );
}
