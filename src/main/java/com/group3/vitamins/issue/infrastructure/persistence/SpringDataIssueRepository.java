package com.group3.vitamins.issue.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataIssueRepository extends JpaRepository<IssueEntity, Long> {
}
