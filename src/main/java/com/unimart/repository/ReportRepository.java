package com.unimart.repository;

import com.unimart.domain.Report;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByListingCommunityIdAndResolvedFalse(Long communityId);
}
