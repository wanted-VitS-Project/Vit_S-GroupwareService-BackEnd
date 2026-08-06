package com.group3.vitamins.issue.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataIssueBlockRepository extends JpaRepository<IssueBlockEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM IssueBlockEntity b WHERE b.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") Long issueId);
}
