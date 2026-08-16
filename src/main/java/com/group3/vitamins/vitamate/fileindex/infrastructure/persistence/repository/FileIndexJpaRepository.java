package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.entity.FileIndexEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// file_index 저장과 file_version 존재 확인을 담당하는 JPA repository입니다.
public interface FileIndexJpaRepository extends JpaRepository<FileIndexEntity, Long> {

    @Query(value = """
        SELECT COUNT(*)
          FROM file_version
         WHERE file_version_id = :fileVersionId
           AND deleted_at IS NULL
        """, nativeQuery = true)
    long countActiveFileVersion(@Param("fileVersionId") Long fileVersionId);

    // PENDING/PROCESSING(비종료) 상태 전이 — dispatch(최초 등록)와 PROCESSING 확인 콜백이 쓴다.
    // retry_count는 여기서 건드리지 않는다: 최초 INSERT는 컬럼 DEFAULT(0)를 그대로 쓰고,
    // 이미 있는 행의 UPDATE는 SET 절에 넣지 않아 기존 값을 그대로 보존한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO file_index (
            file_version_id,
            index_attempt_id,
            index_status,
            index_error_message,
            processing_started_at,
            lease_expires_at,
            indexed_at,
            created_at,
            updated_at,
            deleted_at
        )
        VALUES (
            :fileVersionId,
            :indexAttemptId,
            :indexStatus,
            :errorMessage,
            :processingStartedAt,
            :leaseExpiresAt,
            :indexedAt,
            :now,
            :now,
            NULL
        )
        ON DUPLICATE KEY UPDATE
            index_attempt_id = :indexAttemptId,
            index_status = :indexStatus,
            index_error_message = :errorMessage,
            processing_started_at = :processingStartedAt,
            lease_expires_at = :leaseExpiresAt,
            indexed_at = :indexedAt,
            updated_at = :now,
            deleted_at = NULL
        """, nativeQuery = true)
    int upsertStatus(
            @Param("fileVersionId") Long fileVersionId,
            @Param("indexAttemptId") String indexAttemptId,
            @Param("indexStatus") String indexStatus,
            @Param("errorMessage") String errorMessage,
            @Param("processingStartedAt") LocalDateTime processingStartedAt,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt,
            @Param("indexedAt") LocalDateTime indexedAt,
            @Param("now") LocalDateTime now
    );

    // COMPLETED/FAILED 종료 상태 전이 — 현재 인정된 시도(attemptId)와 일치할 때만 반영된다.
    // 더 이상 점유가 필요 없으니 lease도 함께 비운다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE file_index
           SET index_status = :indexStatus,
               index_error_message = :errorMessage,
               processing_started_at = NULL,
               lease_expires_at = NULL,
               indexed_at = :indexedAt,
               updated_at = :now,
               deleted_at = NULL
         WHERE file_version_id = :fileVersionId
           AND index_attempt_id = :indexAttemptId
           AND deleted_at IS NULL
        """, nativeQuery = true)
    int updateStatusWhenAttemptMatches(
            @Param("fileVersionId") Long fileVersionId,
            @Param("indexAttemptId") String indexAttemptId,
            @Param("indexStatus") String indexStatus,
            @Param("errorMessage") String errorMessage,
            @Param("indexedAt") LocalDateTime indexedAt,
            @Param("now") LocalDateTime now
    );

    // FAILED 판정 직후, 같은 트랜잭션 안에서 재시도 가능(retryable) 판정이면 즉시 PENDING으로
    // 되돌리고 새 attemptId를 발급한다. index_status='FAILED' 조건으로 우리가 방금 실패 처리한
    // 바로 그 행만 대상이 되도록 펜싱한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE file_index
           SET index_status = 'PENDING',
               index_attempt_id = :newAttemptId,
               retry_count = retry_count + 1,
               index_error_message = NULL,
               processing_started_at = :now,
               lease_expires_at = :leaseExpiresAt,
               updated_at = :now
         WHERE file_version_id = :fileVersionId
           AND index_status = 'FAILED'
           AND deleted_at IS NULL
        """, nativeQuery = true)
    int retryAfterFailure(
            @Param("fileVersionId") Long fileVersionId,
            @Param("newAttemptId") String newAttemptId,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT f
          FROM FileIndexEntity f
         WHERE f.fileVersionId = :fileVersionId
           AND f.indexAttemptId = :indexAttemptId
           AND f.deletedAt IS NULL
        """)
    Optional<FileIndexEntity> findCurrentAttemptForUpdate(
            @Param("fileVersionId") Long fileVersionId,
            @Param("indexAttemptId") String indexAttemptId
    );

    // PENDING/PROCESSING인데 lease가 실제로 만료된(=워커가 살아있다는 증거가 사라진) 행을
    // 재시도 후보로 찾는다. 재시도 상한 미만인 것만 대상이다 — 상한을 넘긴 것은
    // findExhaustedFileVersionIdCandidates가 별도로 처리한다. 후보는 재발행 전에
    // claimForRetry로 다시 한번 원자적으로 확인한다(조회~claim 사이의 TOCTOU 방지).
    @Query(value = """
        SELECT file_version_id
          FROM file_index
         WHERE index_status IN ('PENDING', 'PROCESSING')
           AND lease_expires_at IS NOT NULL
           AND lease_expires_at < :now
           AND retry_count < :maxRetryCount
           AND deleted_at IS NULL
         ORDER BY lease_expires_at ASC
         LIMIT :limit
        """, nativeQuery = true)
    List<Long> findReclaimableFileVersionIdCandidates(
            @Param("now") LocalDateTime now,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("limit") int limit
    );

    // lease가 만료됐지만 이미 재시도 상한을 소진한 행 — 더 재발행하지 않고 바로 FAILED로 종료한다.
    @Query(value = """
        SELECT file_version_id
          FROM file_index
         WHERE index_status IN ('PENDING', 'PROCESSING')
           AND lease_expires_at IS NOT NULL
           AND lease_expires_at < :now
           AND retry_count >= :maxRetryCount
           AND deleted_at IS NULL
         ORDER BY lease_expires_at ASC
         LIMIT :limit
        """, nativeQuery = true)
    List<Long> findExhaustedFileVersionIdCandidates(
            @Param("now") LocalDateTime now,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("limit") int limit
    );

    // 후보 조회 이후에도 lease 만료·재시도 가능 조건이 여전히 유효한지 다시 확인하며 원자적으로
    // claim한다. 1건 갱신되면 이 스케줄러가 재시도를 선점한 것이고, 0건이면 그 사이 워커가
    // 정상적으로 완료했거나 다른 경로로 상태가 바뀐 것이므로 건너뛴다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE file_index
           SET index_status = 'PENDING',
               index_attempt_id = :newAttemptId,
               retry_count = retry_count + 1,
               processing_started_at = :now,
               lease_expires_at = :leaseExpiresAt,
               updated_at = :now
         WHERE file_version_id = :fileVersionId
           AND index_status IN ('PENDING', 'PROCESSING')
           AND lease_expires_at IS NOT NULL
           AND lease_expires_at < :now
           AND retry_count < :maxRetryCount
           AND deleted_at IS NULL
        """, nativeQuery = true)
    int claimForRetry(
            @Param("fileVersionId") Long fileVersionId,
            @Param("now") LocalDateTime now,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("newAttemptId") String newAttemptId,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt
    );

    // 재시도 상한을 소진한 채로 lease가 만료된 행을 최종 실패로 종료한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE file_index
           SET index_status = 'FAILED',
               index_error_message = :errorMessage,
               processing_started_at = NULL,
               lease_expires_at = NULL,
               updated_at = :now
         WHERE file_version_id = :fileVersionId
           AND index_status IN ('PENDING', 'PROCESSING')
           AND lease_expires_at IS NOT NULL
           AND lease_expires_at < :now
           AND retry_count >= :maxRetryCount
           AND deleted_at IS NULL
        """, nativeQuery = true)
    int failExhausted(
            @Param("fileVersionId") Long fileVersionId,
            @Param("now") LocalDateTime now,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("errorMessage") String errorMessage
    );
}
