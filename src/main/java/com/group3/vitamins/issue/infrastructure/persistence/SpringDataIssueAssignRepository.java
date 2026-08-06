package com.group3.vitamins.issue.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataIssueAssignRepository extends JpaRepository<IssueAssignEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM IssueAssignEntity a WHERE a.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") Long issueId);
}
