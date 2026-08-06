package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// 문서 청크가 선택 문서 버전에 속하는지 검증하는 JPA Repository
public interface DocumentChunkJpaRepository extends JpaRepository<DocumentChunkEntity, Long> {

    // 청크 ID와 파일 버전 ID가 서로 연결되어 있고 삭제되지 않았는지 확인한다.
    boolean existsByIdAndFileVersionIdAndDeletedAtIsNull(Long id, Long fileVersionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE DocumentChunkEntity c
           SET c.deletedAt = :now,
               c.updatedAt = :now
         WHERE c.fileVersionId = :fileVersionId
           AND c.chunkIndex NOT IN :chunkIndexes
           AND c.deletedAt IS NULL
        """)
    int softDeleteMissingChunks(
            @Param("fileVersionId") Long fileVersionId,
            @Param("chunkIndexes") List<Integer> chunkIndexes,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO document_chunk (
            file_version_id,
            chunk_index,
            page_number,
            section_title,
            start_offset,
            end_offset,
            token_count,
            chroma_id,
            excerpt,
            embedding_model,
            embedding_status,
            created_at,
            updated_at,
            deleted_at
        )
        VALUES (
            :fileVersionId,
            :chunkIndex,
            :pageNumber,
            :sectionTitle,
            :startOffset,
            :endOffset,
            :tokenCount,
            NULL,
            :excerpt,
            NULL,
            'PENDING',
            :now,
            :now,
            NULL
        )
        ON DUPLICATE KEY UPDATE
            page_number = VALUES(page_number),
            section_title = VALUES(section_title),
            start_offset = VALUES(start_offset),
            end_offset = VALUES(end_offset),
            token_count = VALUES(token_count),
            chroma_id = NULL,
            excerpt = VALUES(excerpt),
            embedding_model = NULL,
            embedding_status = 'PENDING',
            updated_at = :now,
            deleted_at = NULL
        """, nativeQuery = true)
    int upsertChunk(
            @Param("fileVersionId") Long fileVersionId,
            @Param("chunkIndex") Integer chunkIndex,
            @Param("pageNumber") Integer pageNumber,
            @Param("sectionTitle") String sectionTitle,
            @Param("startOffset") Integer startOffset,
            @Param("endOffset") Integer endOffset,
            @Param("tokenCount") Integer tokenCount,
            @Param("excerpt") String excerpt,
            @Param("now") LocalDateTime now
    );
}
