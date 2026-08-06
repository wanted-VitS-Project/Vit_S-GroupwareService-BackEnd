package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.entity.FileIndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

// JPA repository for file_index storage and file_version existence checks.
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
            index_status,
            index_error_message,
            indexed_at,
            created_at,
            updated_at,
            deleted_at
        )
        VALUES (
            :fileVersionId,
            :indexStatus,
            :errorMessage,
            :indexedAt,
            :now,
            :now,
            NULL
        )
        ON DUPLICATE KEY UPDATE
            index_status = :indexStatus,
            index_error_message = :errorMessage,
            indexed_at = :indexedAt,
            updated_at = :now,
            deleted_at = NULL
        """, nativeQuery = true)
    int upsertStatus(
            @Param("fileVersionId") Long fileVersionId,
            @Param("indexStatus") String indexStatus,
            @Param("errorMessage") String errorMessage,
            @Param("indexedAt") LocalDateTime indexedAt,
            @Param("now") LocalDateTime now
    );
}
