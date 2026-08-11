package com.group3.vitamins.approval.domain.model;

import java.time.LocalDateTime;

/**
 * 결재 문서 도메인 모델. INV-04: {@code file_version_id} 만 참조 — file_version 과 JPA 연관관계 없음.
 *
 * <p>삭제는 두 경로 모두 논리 삭제다(`DELETE.md` D-1) — APR-007의 DRAFT 수동 연결 해제와
 * DEL-005의 상위 블록 삭제 전파 둘 다 {@code deletedAt}을 기록한다. 파일 영구삭제 잠금은 이 둘을
 * <b>회차 생존</b>으로 구분한다(해제는 회차가 살아 있고, 상위 삭제는 회차도 삭제된다).
 */
public class ApprovalDocument {

    private final Long documentId;
    private final Long revisionId;
    private final Long fileVersionId;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;

    private ApprovalDocument(Long documentId, Long revisionId, Long fileVersionId,
                             LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.documentId = documentId;
        this.revisionId = revisionId;
        this.fileVersionId = fileVersionId;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public static ApprovalDocument reconstruct(Long documentId, Long revisionId, Long fileVersionId,
                                                LocalDateTime createdAt, LocalDateTime deletedAt) {
        return new ApprovalDocument(documentId, revisionId, fileVersionId, createdAt, deletedAt);
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
