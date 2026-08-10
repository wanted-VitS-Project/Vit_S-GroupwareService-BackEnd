package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTaskStatus;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunTaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataCollectionRunTaskRepository
        extends JpaRepository<CollectionRunTaskJpaEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CollectionRunTaskJpaEntity task
           SET task.taskStatus = :processingStatus,
               task.attemptId = :attemptId,
               task.retryCount = :retryCount,
               task.processingStartedAt = :startedAt,
               task.leaseExpiresAt = :leaseExpiresAt,
               task.errorCode = null,
               task.errorMessage = null,
               task.finishedAt = null,
               task.updatedAt = :startedAt
         WHERE task.crawlRunId = :runId
           AND task.noticeType = :noticeType
           AND task.keyword = :keyword
           AND task.regionCode = :regionCode
           AND task.industryCode = :industryCode
           AND task.pageNumber = :pageNumber
           AND (
                task.taskStatus = :pendingStatus
                OR (task.taskStatus = :processingStatus AND task.leaseExpiresAt < :startedAt)
           )
        """)
    int claimTask(
            Long runId,
            BidNoticeType noticeType,
            String keyword,
            String regionCode,
            String industryCode,
            int pageNumber,
            String attemptId,
            int retryCount,
            LocalDateTime startedAt,
            LocalDateTime leaseExpiresAt,
            CollectionRunTaskStatus pendingStatus,
            CollectionRunTaskStatus processingStatus
    );

    Optional<CollectionRunTaskJpaEntity>
    findByCrawlRunIdAndAttemptIdAndTaskStatus(
            Long runId,
            String attemptId,
            CollectionRunTaskStatus taskStatus
    );

    @Query("""
        SELECT task
          FROM CollectionRunTaskJpaEntity task
         WHERE task.crawlRunId = :runId
           AND (
                task.taskStatus = :pendingStatus
                OR (
                    task.taskStatus = :processingStatus
                    AND task.leaseExpiresAt < :now
                )
           )
         ORDER BY task.pageNumber ASC, task.crawlRunTaskId ASC
        """)
    List<CollectionRunTaskJpaEntity> findProcessableTasks(
            Long runId,
            LocalDateTime now,
            CollectionRunTaskStatus pendingStatus,
            CollectionRunTaskStatus processingStatus,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CollectionRunTaskJpaEntity task
           SET task.taskStatus = :completedStatus,
               task.collectedCount = :collectedCount,
               task.insertedCount = :insertedCount,
               task.updatedCount = :updatedCount,
               task.skippedCount = :skippedCount,
               task.leaseExpiresAt = null,
               task.errorCode = null,
               task.errorMessage = null,
               task.finishedAt = :finishedAt,
               task.updatedAt = :finishedAt
         WHERE task.crawlRunTaskId = :taskId
           AND task.attemptId = :attemptId
           AND task.taskStatus = :processingStatus
        """)
    int completeTask(
            Long taskId,
            String attemptId,
            int collectedCount,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            LocalDateTime finishedAt,
            CollectionRunTaskStatus processingStatus,
            CollectionRunTaskStatus completedStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CollectionRunTaskJpaEntity task
           SET task.taskStatus = :pendingStatus,
               task.attemptId = null,
               task.processingStartedAt = null,
               task.leaseExpiresAt = null,
               task.errorCode = :errorCode,
               task.errorMessage = :errorMessage,
               task.updatedAt = :updatedAt
         WHERE task.crawlRunTaskId = :taskId
           AND task.attemptId = :attemptId
           AND task.taskStatus = :processingStatus
        """)
    int prepareRetry(
            Long taskId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime updatedAt,
            CollectionRunTaskStatus pendingStatus,
            CollectionRunTaskStatus processingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CollectionRunTaskJpaEntity task
           SET task.taskStatus = :failedStatus,
               task.leaseExpiresAt = null,
               task.errorCode = :errorCode,
               task.errorMessage = :errorMessage,
               task.finishedAt = :finishedAt,
               task.updatedAt = :finishedAt
         WHERE task.crawlRunTaskId = :taskId
           AND task.attemptId = :attemptId
           AND task.taskStatus = :processingStatus
        """)
    int failTask(
            Long taskId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime finishedAt,
            CollectionRunTaskStatus processingStatus,
            CollectionRunTaskStatus failedStatus
    );

    List<CollectionRunTaskJpaEntity> findAllByCrawlRunId(Long runId);
}
