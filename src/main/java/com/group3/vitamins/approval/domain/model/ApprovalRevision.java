package com.group3.vitamins.approval.domain.model;

import java.time.LocalDateTime;

/**
 * 결재 상신(회차) 도메인 모델. INV-02: 상신 이후({@code status != DRAFT}) 이 값은 바뀌지 않는다.
 */
public class ApprovalRevision {

    private final Long revisionId;
    private final Long approvalId;
    private final int revisionNo;
    private final String title;
    private final String content;
    private final ApprovalStatus status;
    private final LocalDateTime submittedAt;
    private final LocalDateTime finishedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private ApprovalRevision(Long revisionId, Long approvalId, int revisionNo, String title, String content,
                              ApprovalStatus status, LocalDateTime submittedAt, LocalDateTime finishedAt,
                              LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.revisionId = revisionId;
        this.approvalId = approvalId;
        this.revisionNo = revisionNo;
        this.title = title;
        this.content = content;
        this.status = status;
        this.submittedAt = submittedAt;
        this.finishedAt = finishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static ApprovalRevision reconstruct(Long revisionId, Long approvalId, int revisionNo,
                                                String title, String content, ApprovalStatus status,
                                                LocalDateTime submittedAt, LocalDateTime finishedAt,
                                                LocalDateTime createdAt, LocalDateTime updatedAt,
                                                LocalDateTime deletedAt) {
        return new ApprovalRevision(revisionId, approvalId, revisionNo, title, content, status,
                submittedAt, finishedAt, createdAt, updatedAt, deletedAt);
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public Long getApprovalId() {
        return approvalId;
    }

    public int getRevisionNo() {
        return revisionNo;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
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
