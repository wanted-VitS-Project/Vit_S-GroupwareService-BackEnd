package com.group3.vitamins.approval.infrastructure.persistence;

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

/**
 * 결재 문서. 팀 ERD 의 {@code approval_document} 테이블 (`APR-V1.md` §2-A).
 *
 * <p>INV-04: {@code file_version_id} 만 참조한다 — 파일 실물 저장은 파일 도메인 소관이라
 * 여기서 {@code file_version} 과 JPA 연관관계를 맺지 않는다.
 */
@Entity
@Table(name = "approval_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_document_id")
    private Long approvalDocumentId;

    @Column(name = "approval_revision_id", nullable = false)
    private Long approvalRevisionId;

    @Column(name = "file_version_id", nullable = false)
    private Long fileVersionId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 문서 추가 (APR-005) — 호출 전 {@code file_version.upload_status == COMPLETED} 검증은 서비스 책임 */
    public static ApprovalDocumentEntity create(Long approvalRevisionId, Long fileVersionId) {
        ApprovalDocumentEntity document = new ApprovalDocumentEntity();
        document.approvalRevisionId = approvalRevisionId;
        document.fileVersionId = fileVersionId;
        return document;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
