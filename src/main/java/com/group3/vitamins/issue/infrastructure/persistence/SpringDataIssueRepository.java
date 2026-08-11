package com.group3.vitamins.issue.infrastructure.persistence;

import com.group3.vitamins.issue.domain.IssuePriority;
import com.group3.vitamins.issue.domain.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SpringDataIssueRepository extends JpaRepository<IssueEntity, Long> {

    Optional<IssueEntity> findByIssueIdAndDeletedAtIsNull(Long issueId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE IssueEntity i
               SET i.title = :title,
                   i.content = :content,
                   i.dueDate = :dueDate,
                   i.priority = :priority,
                   i.version = i.version + 1
             WHERE i.issueId = :issueId
               AND i.version = :expectedVersion
               AND i.deletedAt IS NULL
            """)
    int updateFieldsIfVersionMatches(
            @Param("issueId") Long issueId,
            @Param("title") String title,
            @Param("content") String content,
            @Param("dueDate") LocalDateTime dueDate,
            @Param("priority") IssuePriority priority,
            @Param("expectedVersion") int expectedVersion
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE IssueEntity i
               SET i.version = i.version + 1
             WHERE i.issueId = :issueId
               AND i.version = :expectedVersion
               AND i.deletedAt IS NULL
            """)
    int touchIfVersionMatches(
            @Param("issueId") Long issueId,
            @Param("expectedVersion") int expectedVersion
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE IssueEntity i
               SET i.status = :status,
                   i.finishDay = :completedAt,
                   i.version = i.version + 1
             WHERE i.issueId = :issueId
               AND i.version = :expectedVersion
               AND i.deletedAt IS NULL
            """)
    int changeStatusIfVersionMatches(
            @Param("issueId") Long issueId,
            @Param("status") IssueStatus status,
            @Param("completedAt") LocalDateTime completedAt,
            @Param("expectedVersion") int expectedVersion
    );

    /** issue_assign·issue_block은 issue_id에 ON DELETE CASCADE가 걸려 있어 이 삭제 하나로 같이 정리된다. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM IssueEntity i WHERE i.deletedAt < :threshold")
    int hardDeleteByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
