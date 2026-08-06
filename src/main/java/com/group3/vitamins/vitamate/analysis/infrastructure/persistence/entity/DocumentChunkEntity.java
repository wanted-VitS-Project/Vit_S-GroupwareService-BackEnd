package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 비타메이트 citation 검증에 필요한 문서 청크 최소 JPA 엔티티
@Getter
@Entity
@Table(name = "document_chunk")
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
}
