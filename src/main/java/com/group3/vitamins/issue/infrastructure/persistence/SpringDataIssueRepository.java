package com.group3.vitamins.issue.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SpringDataIssueRepository extends JpaRepository<IssueEntity, Long> {

    Optional<IssueEntity> findByIssueIdAndDeletedAtIsNull(Long issueId);

    /** issue_assign·issue_block은 issue_id에 ON DELETE CASCADE가 걸려 있어 이 삭제 하나로 같이 정리된다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM IssueEntity i WHERE i.deletedAt < :threshold")
    int hardDeleteByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
