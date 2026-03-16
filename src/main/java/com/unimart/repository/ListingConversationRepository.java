package com.unimart.repository;

import com.unimart.domain.ListingConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingConversationRepository extends JpaRepository<ListingConversation, Long> {
    Optional<ListingConversation> findByListingIdAndBuyerId(Long listingId, Long buyerId);
    List<ListingConversation> findBySellerIdAndListingCommunityIdInOrderByLastMessageAtDesc(Long sellerId, List<Long> communityIds);
    List<ListingConversation> findByListingIdOrderByLastMessageAtDesc(Long listingId);
    List<ListingConversation> findByBuyerIdAndListingCommunityIdInOrderByLastMessageAtDesc(Long buyerId, List<Long> communityIds);
}
