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

    // citation 검증에서 청크마다 존재 여부를 반복 조회하지 않도록, 활성 청크를 한 번에 조회한다.
    List<DocumentChunkEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    // Python worker가 ChromaDB에 저장할 수 있도록 방금 저장된 chunk ID 목록을 조회합니다.
    @Query("""
        SELECT c
          FROM DocumentChunkEntity c
         WHERE c.fileVersionId = :fileVersionId
           AND c.chunkIndex IN :chunkIndexes
           AND c.deletedAt IS NULL
         ORDER BY c.chunkIndex ASC
        """)
    List<DocumentChunkEntity> findActiveByFileVersionIdAndChunkIndexIn(
            @Param("fileVersionId") Long fileVersionId,
            @Param("chunkIndexes") List<Integer> chunkIndexes
    );

    // 임베딩 결과 요청의 모든 chunk가 같은 fileVersionId에 속하는지 확인합니다.
    @Query("""
        SELECT COUNT(c)
          FROM DocumentChunkEntity c
         WHERE c.fileVersionId = :fileVersionId
           AND c.id IN :documentChunkIds
           AND c.deletedAt IS NULL
        """)
    long countActiveByFileVersionIdAndIdIn(
            @Param("fileVersionId") Long fileVersionId,
            @Param("documentChunkIds") List<Long> documentChunkIds
    );

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

}
