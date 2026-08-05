package com.group3.vitamins.approval.domain.model;

import java.time.LocalDateTime;

/**
 * 결재 문서 도메인 모델. INV-04: {@code file_version_id} 만 참조 — file_version 과 JPA 연관관계 없음.
 *
 * <p>APR-007: 제거는 항상 하드 삭제(이력 보존 대상 아님) — {@code deletedAt} 을 두지 않는다.
 */
public class ApprovalDocument {

    private final Long documentId;
    private final Long revisionId;
    private final Long fileVersionId;
    private final LocalDateTime createdAt;

    private ApprovalDocument(Long documentId, Long revisionId, Long fileVersionId, LocalDateTime createdAt) {
        this.documentId = documentId;
        this.revisionId = revisionId;
        this.fileVersionId = fileVersionId;
        this.createdAt = createdAt;
    }

    public static ApprovalDocument reconstruct(Long documentId, Long revisionId, Long fileVersionId,
                                                LocalDateTime createdAt) {
        return new ApprovalDocument(documentId, revisionId, fileVersionId, createdAt);
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public Long getFileVersionId() {
        return fileVersionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
