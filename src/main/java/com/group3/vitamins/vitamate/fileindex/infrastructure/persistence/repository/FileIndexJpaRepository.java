package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.entity.FileIndexEntity;
import jakarta.persistence.LockModeType;
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

    // PENDING으로 등록됐지만 오래 방치된 행을 찾는다 — dispatch 트랜잭션이 커밋된 뒤 큐 발행
    // 전에 프로세스가 죽으면(afterCommit 유실) 이 상태로 영원히 멈출 수 있어, 재발행 스케줄러가 사용한다.
    @Query("""
        SELECT f.fileVersionId
          FROM FileIndexEntity f
         WHERE f.indexStatus = com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus.PENDING
           AND f.updatedAt < :before
           AND f.deletedAt IS NULL
        """)
    List<Long> findStalePendingFileVersionIds(@Param("before") LocalDateTime before);
}
