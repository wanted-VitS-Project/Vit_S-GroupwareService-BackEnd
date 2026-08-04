package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.ApprovalLineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결재선. 팀 ERD 의 {@code approval_line} 테이블 (`APR-V1.md` §2-A·§2-C).
 *
 * <p>INV-01: {@code MASTER} 여부와 무관하다 — 최종 결재자는 그냥 {@code sequence_no} 최댓값.
 * INV-11: 부서·직책은 여기 저장하지 않는다. 조회 시 {@code employee} 라이브 조회로 채운다.
 */
@Entity
@Table(name = "approval_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_line_id")
    private Long approvalLineId;

    /** 결재자 사번 */
    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

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

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 결재선 등록 (APR-009) — DRAFT 편집 중에는 전부 {@code DRAFT} 상태 */
    public static ApprovalLineEntity create(Long approvalRevisionId, String userId, int sequenceNo) {
        ApprovalLineEntity line = new ApprovalLineEntity();
        line.approvalRevisionId = approvalRevisionId;
        line.userId = userId;
        line.sequenceNo = sequenceNo;
        line.status = ApprovalLineStatus.DRAFT;
        return line;
    }

    public boolean isActive() {
        return status == ApprovalLineStatus.ACTIVE;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
