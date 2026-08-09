package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupJob;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VitamateCleanupJobJpaRepository
        extends JpaRepository<VitamateCleanupJobEntity, Long> {

    // PUBLISHED 상태의 현재 시도만 PROCESSING으로 전환합니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE VitamateCleanupJobEntity job
               SET job.cleanupStatus = :nextStatus,
                   job.processingStartedAt = :processingStartedAt,
                   job.updatedAt = :processingStartedAt
             WHERE job.cleanupJobId = :cleanupJobId
               AND job.currentAttemptId = :attemptId
               AND job.cleanupStatus = :expectedStatus
            """)
    int markProcessing(
            @Param("cleanupJobId") Long cleanupJobId,
            @Param("attemptId") String attemptId,
            @Param("expectedStatus") VitamateCleanupJob.Status expectedStatus,
            @Param("nextStatus") VitamateCleanupJob.Status nextStatus,
            @Param("processingStartedAt") LocalDateTime processingStartedAt
    );

    // PUBLISHED 또는 PROCESSING 상태의 현재 시도만 COMPLETED로 전환합니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE VitamateCleanupJobEntity job
               SET job.cleanupStatus = :nextStatus,
                   job.deletedVectorCount = :deletedVectorCount,
                   job.lastErrorCode = null,
                   job.lastErrorMessage = null,
                   job.completedAt = :completedAt,
                   job.updatedAt = :completedAt
             WHERE job.cleanupJobId = :cleanupJobId
               AND job.currentAttemptId = :attemptId
               AND job.cleanupStatus IN :allowedStatuses
            """)
    int markCompleted(
            @Param("cleanupJobId") Long cleanupJobId,
            @Param("attemptId") String attemptId,
            @Param("deletedVectorCount") int deletedVectorCount,
            @Param("allowedStatuses") Iterable<VitamateCleanupJob.Status> allowedStatuses,
            @Param("nextStatus") VitamateCleanupJob.Status nextStatus,
            @Param("completedAt") LocalDateTime completedAt
    );

    // 현재 시도의 실패를 재시도 대기 상태로 전환합니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE VitamateCleanupJobEntity job
               SET job.cleanupStatus = :nextStatus,
                   job.currentAttemptId = null,
                   job.nextRetryAt = :nextRetryAt,
                   job.lastErrorCode = :errorCode,
                   job.lastErrorMessage = :errorMessage,
                   job.updatedAt = :updatedAt
             WHERE job.cleanupJobId = :cleanupJobId
               AND job.currentAttemptId = :attemptId
               AND job.cleanupStatus IN :allowedStatuses
               AND job.attemptCount < :maxAttempts
            """)
    int markRetryWaiting(
            @Param("cleanupJobId") Long cleanupJobId,
            @Param("attemptId") String attemptId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("maxAttempts") int maxAttempts,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("allowedStatuses") Iterable<VitamateCleanupJob.Status> allowedStatuses,
            @Param("nextStatus") VitamateCleanupJob.Status nextStatus
    );

    // 현재 시도의 실패를 최종 실패 상태로 전환합니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE VitamateCleanupJobEntity job
               SET job.cleanupStatus = :nextStatus,
                   job.lastErrorCode = :errorCode,
                   job.lastErrorMessage = :errorMessage,
                   job.completedAt = :completedAt,
                   job.updatedAt = :completedAt
             WHERE job.cleanupJobId = :cleanupJobId
               AND job.currentAttemptId = :attemptId
               AND job.cleanupStatus IN :allowedStatuses
            """)
    int markDeadLetter(
            @Param("cleanupJobId") Long cleanupJobId,
            @Param("attemptId") String attemptId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("allowedStatuses") Iterable<VitamateCleanupJob.Status> allowedStatuses,
            @Param("nextStatus") VitamateCleanupJob.Status nextStatus,
            @Param("completedAt") LocalDateTime completedAt
    );

    // callback이 거부됐을 때 현재 상태를 반환합니다.
    @Query("""
            SELECT job.cleanupStatus
              FROM VitamateCleanupJobEntity job
             WHERE job.cleanupJobId = :cleanupJobId
            """)
    Optional<VitamateCleanupJob.Status> findCleanupStatusById(
            @Param("cleanupJobId") Long cleanupJobId
    );

    // callback 재시도 정책에 사용할 현재 처리 시도 횟수를 반환합니다.
    @Query("""
            SELECT job.attemptCount
              FROM VitamateCleanupJobEntity job
             WHERE job.cleanupJobId = :cleanupJobId
            """)
    Optional<Integer> findAttemptCountById(
            @Param("cleanupJobId") Long cleanupJobId
    );
}
