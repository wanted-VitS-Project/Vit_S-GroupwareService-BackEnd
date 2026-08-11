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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO file_index (
            file_version_id,
            index_attempt_id,
            index_status,
            index_error_message,
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
            :indexedAt,
            :now,
            :now,
            NULL
        )
        ON DUPLICATE KEY UPDATE
            index_attempt_id = :indexAttemptId,
            index_status = :indexStatus,
            index_error_message = :errorMessage,
            indexed_at = :indexedAt,
            updated_at = :now,
            deleted_at = NULL
        """, nativeQuery = true)
    int upsertStatus(
            @Param("fileVersionId") Long fileVersionId,
            @Param("indexAttemptId") String indexAttemptId,
            @Param("indexStatus") String indexStatus,
            @Param("errorMessage") String errorMessage,
            @Param("indexedAt") LocalDateTime indexedAt,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE file_index
           SET index_status = :indexStatus,
               index_error_message = :errorMessage,
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

    // PENDING으로 등록됐지만 오래 방치된 행을 후보로 찾는다(제한된 배치, 오래된 것부터) —
    // dispatch 트랜잭션이 커밋된 뒤 큐 발행 전에 프로세스가 죽으면(afterCommit 유실) 이 상태로
    // 영원히 멈출 수 있어, 재발행 스케줄러가 사용한다. 이 목록은 후보일 뿐이며 실제 재발행 전에
    // claimStalePending으로 다시 한번 원자적으로 확인한다 — 조회 시점과 발행 시점 사이에
    // worker가 이미 PROCESSING/COMPLETED로 넘겼을 수 있기 때문이다.
    @Query("""
        SELECT f.fileVersionId
          FROM FileIndexEntity f
         WHERE f.indexStatus = com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus.PENDING
           AND f.updatedAt < :before
           AND f.deletedAt IS NULL
         ORDER BY f.updatedAt ASC
        """)
    List<Long> findStalePendingFileVersionIdCandidates(@Param("before") LocalDateTime before, Pageable pageable);

    // 후보를 재발행하기 직전에 PENDING·stale 조건을 다시 검사하며 원자적으로 선점(claim)한다.
    // updatedAt만 갱신해 "지금 이 순간에도 여전히 PENDING이고 그 사이 아무도 안 건드렸다"를
    // 보장한다 — 1건이 갱신되면 선점 성공, 0건이면 이미 다른 경로로 상태가 바뀐 것이므로 건너뛴다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE FileIndexEntity f
           SET f.updatedAt = :now
         WHERE f.fileVersionId = :fileVersionId
           AND f.indexStatus = com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus.PENDING
           AND f.updatedAt < :before
           AND f.deletedAt IS NULL
        """)
    int claimStalePending(
            @Param("fileVersionId") Long fileVersionId,
            @Param("before") LocalDateTime before,
            @Param("now") LocalDateTime now
    );
}
