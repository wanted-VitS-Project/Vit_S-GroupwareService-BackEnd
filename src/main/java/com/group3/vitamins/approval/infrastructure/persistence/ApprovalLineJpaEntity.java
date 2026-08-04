package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 결재선. 팀 ERD 의 {@code approval_line} 테이블 (`APR-V1.md` §2-A·§2-C).
 *
 * <p>INV-01: {@code MASTER} 여부와 무관하다 — 최종 결재자는 그냥 {@code sequenceNo} 최댓값.
 * INV-11: 부서·직책 컬럼이 없다 — 조회 시 {@code employee} 라이브 조회로 채운다.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "approval_line")
public class ApprovalLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_line_id")
    private Long approvalLineId;

    /** 결재자 사번 */
    @Column(name = "user_id", nullable = false, length = 20)
    private String approverId;

    @Column(name = "approval_revision_id", nullable = false)
    private Long approvalRevisionId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "enum('DRAFT','WAITING','ACTIVE','APPROVED','REJECTED','CANCELED')")
    private ApprovalLineStatus status;

    @Column(name = "opinion", columnDefinition = "TEXT")
    private String opinion;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 결재선 등록(APR-009) — DRAFT 편집 중에는 전부 {@code DRAFT} 상태 */
    public static ApprovalLineJpaEntity createDraft(Long approvalRevisionId, String approverId, int sequenceNo) {
        ApprovalLineJpaEntity entity = new ApprovalLineJpaEntity();
        entity.approvalRevisionId = approvalRevisionId;
        entity.approverId = approverId;
        entity.sequenceNo = sequenceNo;
        entity.status = ApprovalLineStatus.DRAFT;
        return entity;
    }
}
