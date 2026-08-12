package com.group3.vitamins.approval.domain.model;

import java.time.LocalDateTime;

/**
 * 결재 도메인 모델 — 영속성 프레임워크에 의존하지 않는다 (`text.domain.model.Text`와 동일한 구조).
 *
 * <p>{@code blockId} 는 공용 block 테이블을 참조하는 값만 저장할 뿐 FK 는 아니다(INV-10).
 * 이 도메인은 {@code Block} 과 JPA 연관관계를 맺지 않는다.
 */
public class Approval {

    private final Long approvalId;
    private final Long blockId;
    private final String drafterId;
    private final String actingDrafterId;
    private final ApprovalStatus status;
    private final int currentRevisionNo;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private Approval(Long approvalId, Long blockId, String drafterId, String actingDrafterId, ApprovalStatus status,
                      int currentRevisionNo, LocalDateTime completedAt,
                      LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.approvalId = approvalId;
        this.blockId = blockId;
        this.drafterId = drafterId;
        this.actingDrafterId = actingDrafterId;
        this.status = status;
        this.currentRevisionNo = currentRevisionNo;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Approval reconstruct(Long approvalId, Long blockId, String drafterId, String actingDrafterId,
                                        ApprovalStatus status,
                                        int currentRevisionNo, LocalDateTime completedAt,
                                        LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return new Approval(approvalId, blockId, drafterId, actingDrafterId, status, currentRevisionNo,
                completedAt, createdAt, updatedAt, deletedAt);
    }

    public Long getApprovalId() {
        return approvalId;
    }

    public Long getBlockId() {
        return blockId;
    }

    public String getDrafterId() {
        return drafterId;
    }

    public String getActingDrafterId() {
        return actingDrafterId;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public int getCurrentRevisionNo() {
        return currentRevisionNo;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
