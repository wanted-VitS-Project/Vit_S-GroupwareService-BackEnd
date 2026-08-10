package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface SpringDataCollectionRunRepository
        extends JpaRepository<CollectionRunJpaEntity, Long> {

    // 같은 조건에 아직 종료되지 않은 실행이 존재하는지 확인합니다.
    boolean existsByCrawlCondition_CrawlConditionIdAndRunStatusInAndDeletedAtIsNull(
            Long conditionId,
            Collection<CollectionRunStatus> statuses
    );

    // 실행 ID와 조건의 회사 ID가 모두 일치하는 실행만 조회합니다.
    @EntityGraph(attributePaths = "crawlCondition")
    Optional<CollectionRunJpaEntity>
    findByCrawlRunIdAndCrawlCondition_CompanyIdAndDeletedAtIsNull(
            Long runId,
            Long companyId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CollectionRunJpaEntity run
           SET run.runStatus = :processingStatus,
               run.processingAttemptId = :attemptId,
               run.retryCount = :retryCount,
               run.processingStartedAt = :startedAt,
               run.leaseExpiresAt = :leaseExpiresAt,
               run.updatedAt = :startedAt
         WHERE run.crawlRunId = :runId
           AND run.crawlCondition.companyId = :companyId
           AND run.runStatus = :pendingStatus
           AND run.deletedAt IS NULL
        """)
    int claimPendingRun(
            Long runId,
            Long companyId,
            String attemptId,
            int retryCount,
            LocalDateTime startedAt,
            LocalDateTime leaseExpiresAt,
            CollectionRunStatus pendingStatus,
            CollectionRunStatus processingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE CollectionRunJpaEntity run
           SET run.runStatus = :pendingStatus,
               run.processingAttemptId = null,
               run.processingStartedAt = null,
               run.leaseExpiresAt = null,
               run.errorCode = :errorCode,
               run.errorMessage = :errorMessage,
               run.updatedAt = :updatedAt
         WHERE run.crawlRunId = :runId
           AND run.processingAttemptId = :attemptId
           AND run.runStatus = :processingStatus
           AND run.deletedAt IS NULL
        """)
    int prepareRetry(
            Long runId,
            String attemptId,
            String errorCode,
            String errorMessage,
            LocalDateTime updatedAt,
            CollectionRunStatus pendingStatus,
            CollectionRunStatus processingStatus
    );

    @EntityGraph(attributePaths = "crawlCondition")
    Optional<CollectionRunJpaEntity>
    findByCrawlRunIdAndCrawlCondition_CompanyIdAndProcessingAttemptIdAndRunStatusAndDeletedAtIsNull(
            Long runId,
            Long companyId,
            String attemptId,
            CollectionRunStatus runStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE CollectionRunJpaEntity run
       SET run.leaseExpiresAt = :leaseExpiresAt,
           run.updatedAt = :updatedAt
     WHERE run.crawlRunId = :runId
       AND run.processingAttemptId = :attemptId
       AND run.runStatus = :processingStatus
       AND run.deletedAt IS NULL
    """)
    int renewLease(Long runId, String attemptId, LocalDateTime leaseExpiresAt,
                   LocalDateTime updatedAt, CollectionRunStatus processingStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE CollectionRunJpaEntity run
       SET run.runStatus = :finalStatus,
           run.collectedCount = :collectedCount,
           run.insertedCount = :insertedCount,
           run.updatedCount = :updatedCount,
           run.skippedCount = :skippedCount,
           run.finishedAt = :finishedAt,
           run.leaseExpiresAt = null,
           run.errorCode = null,
           run.errorMessage = null,
           run.updatedAt = :finishedAt
     WHERE run.crawlRunId = :runId
       AND run.processingAttemptId = :attemptId
       AND run.runStatus = :processingStatus
       AND run.deletedAt IS NULL
    """)
    int completeRun(Long runId, String attemptId, CollectionRunStatus finalStatus,
                    int collectedCount, int insertedCount, int updatedCount,
                    int skippedCount, LocalDateTime finishedAt,
                    CollectionRunStatus processingStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE CollectionRunJpaEntity run
       SET run.runStatus = :failedStatus,
           run.finishedAt = :finishedAt,
           run.leaseExpiresAt = null,
           run.errorCode = :errorCode,
           run.errorMessage = :errorMessage,
           run.updatedAt = :finishedAt
     WHERE run.crawlRunId = :runId
       AND run.processingAttemptId = :attemptId
       AND run.runStatus = :processingStatus
       AND run.deletedAt IS NULL
    """)
    int failRun(Long runId, String attemptId, String errorCode,
                String errorMessage, LocalDateTime finishedAt,
                CollectionRunStatus processingStatus,
                CollectionRunStatus failedStatus);
}
