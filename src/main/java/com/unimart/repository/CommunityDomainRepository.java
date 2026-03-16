package com.unimart.repository;

import com.unimart.domain.CommunityDomain;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityDomainRepository extends JpaRepository<CommunityDomain, Long> {
    List<CommunityDomain> findByEmailDomain(String emailDomain);
    List<CommunityDomain> findByCommunityId(Long communityId);
}
