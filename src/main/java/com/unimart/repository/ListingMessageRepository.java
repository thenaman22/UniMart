package com.unimart.repository;

import com.unimart.domain.ListingMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingMessageRepository extends JpaRepository<ListingMessage, Long> {
    List<ListingMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    List<ListingMessage> findByConversationIdIn(List<Long> conversationIds);
    Optional<ListingMessage> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
