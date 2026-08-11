package com.group3.vitamins.approval.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 결재 문서. 팀 ERD 의 {@code approval_document} 테이블 (`APR-V1.md` §2-A).
 *
 * <p>INV-04: {@code file_version_id} 만 참조한다 — 파일 실물 저장은 파일 도메인 소관이라
 * 여기서 {@code file_version} 과 JPA 연관관계를 맺지 않는다.
 *
 * <p>삭제는 두 경로 모두 논리 삭제다 — APR-007의 DRAFT 수동 연결 해제와 DEL-005의 상위 블록 삭제
 * 전파 둘 다 {@code deleted_at}을 기록한다. 파일 영구삭제 잠금은 이 둘을 <b>회차 생존</b>으로
 * 구분한다(수동 해제는 회차가 살아 있고, 상위 삭제는 회차도 삭제된다).
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "approval_document")
public class ApprovalDocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_document_id")
    private Long approvalDocumentId;

    @Column(name = "approval_revision_id", nullable = false)
    private Long approvalRevisionId;

    @Column(name = "file_version_id", nullable = false)
    private Long fileVersionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 문서 추가(APR-005) · 재상신 복사(SUB-006) 공용 생성 */
    public static ApprovalDocumentJpaEntity create(Long approvalRevisionId, Long fileVersionId) {
        ApprovalDocumentJpaEntity entity = new ApprovalDocumentJpaEntity();
        entity.approvalRevisionId = approvalRevisionId;
        entity.fileVersionId = fileVersionId;
        return entity;
    }
}
