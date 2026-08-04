package com.group3.vitamins.approval.domain.model;

import java.time.LocalDateTime;

/**
 * 결재선 도메인 모델. INV-01: {@code MASTER} 여부와 무관하다 — 최종 결재자는 그냥 {@code sequenceNo} 최댓값.
 * INV-11: 부서·직책은 여기 없다 — 조회 시 {@code employee} 라이브 조회로 채운다.
 */
public class ApprovalLine {

    private final Long lineId;
    private final Long revisionId;
    private final String approverId;
    private final int sequenceNo;
    private final ApprovalLineStatus status;
    private final String opinion;
    private final LocalDateTime processedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;

    private ApprovalLine(Long lineId, Long revisionId, String approverId, int sequenceNo,
                          ApprovalLineStatus status, String opinion, LocalDateTime processedAt,
                          LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.lineId = lineId;
        this.revisionId = revisionId;
        this.approverId = approverId;
        this.sequenceNo = sequenceNo;
        this.status = status;
        this.opinion = opinion;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public static ApprovalLine reconstruct(Long lineId, Long revisionId, String approverId, int sequenceNo,
                                            ApprovalLineStatus status, String opinion, LocalDateTime processedAt,
                                            LocalDateTime createdAt, LocalDateTime deletedAt) {
        return new ApprovalLine(lineId, revisionId, approverId, sequenceNo, status, opinion,
                processedAt, createdAt, deletedAt);
    }

    public Long getLineId() {
        return lineId;
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public String getApproverId() {
        return approverId;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public ApprovalLineStatus getStatus() {
        return status;
    }

    public String getOpinion() {
        return opinion;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
