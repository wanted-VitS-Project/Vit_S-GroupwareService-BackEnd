package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 비타메이트 citation 검증에 필요한 문서 청크 최소 JPA 엔티티
@Getter
@Entity
@Table(
        name = "document_chunk",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_chunk_version_index",
                        columnNames = {"file_version_id", "chunk_index"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_chunk_id")
    private Long id;

    @Column(name = "file_version_id", nullable = false)
    private Long fileVersionId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "section_title")
    private String sectionTitle;

    @Column(name = "start_offset")
    private Integer startOffset;

    @Column(name = "end_offset")
    private Integer endOffset;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "chroma_id")
    private String chromaId;

    @Column(name = "excerpt", length = 1000)
    private String excerpt;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "embedding_status", nullable = false)
    private String embeddingStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 청크 저장 생성자만 추가하는 방향
    public DocumentChunkEntity(
            Long fileVersionId,
            Integer chunkIndex,
            Integer pageNumber,
            String sectionTitle,
            Integer startOffset,
            Integer endOffset,
            Integer tokenCount,
            String excerpt,
            LocalDateTime now
    ) {
        this.fileVersionId = fileVersionId;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.sectionTitle = sectionTitle;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenCount = tokenCount;
        this.excerpt = excerpt;
        this.embeddingStatus = "PENDING";
        this.createdAt = now;
        this.updatedAt = now;
    }
}
