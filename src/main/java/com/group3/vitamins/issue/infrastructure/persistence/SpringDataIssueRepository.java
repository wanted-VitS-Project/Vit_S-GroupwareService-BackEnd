package com.group3.vitamins.issue.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataIssueRepository extends JpaRepository<IssueEntity, Long> {

    Optional<IssueEntity> findByIssueIdAndDeletedAtIsNull(Long issueId);
}
