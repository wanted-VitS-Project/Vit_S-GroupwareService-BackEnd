package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.ApprovalStatus;
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
 * 결재 상신(회차). 팀 ERD 의 {@code approval_revision} 테이블 (`APR-V1.md` §2-A·§2-B).
 *
 * <p>INV-02: 상신({@code status != DRAFT}) 이후 이 행은 수정되지 않는다 — 재상신은
 * {@code revision_no + 1} 새 행을 만든다(SUB-005~008). "무엇을 승인했는지"가 영구히
 * 특정되어야 하기 때문.
 */
@Entity
@Table(name = "approval_revision")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_revision_id")
    private Long approvalRevisionId;

    /** 소속 {@code approval}. 도메인 내부 참조라 FK 는 있지만 JPA 연관관계는 맺지 않는다 */
    @Column(name = "approval_id", nullable = false)
    private Long approvalId;

    @Column(name = "revision_no", nullable = false)
    private int revisionNo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "enum('DRAFT','IN_PROGRESS','REJECTED','COMPLETED','CANCELED')")
    private ApprovalStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 최초 1회차 DRAFT 생성 (APR-001) */
    public static ApprovalRevisionEntity startDraft(Long approvalId, int revisionNo, String title, String content) {
        ApprovalRevisionEntity revision = new ApprovalRevisionEntity();
        revision.approvalId = approvalId;
        revision.revisionNo = revisionNo;
        revision.title = title;
        revision.content = content;
        revision.status = ApprovalStatus.DRAFT;
        return revision;
    }

    /** 재상신 준비 (SUB-005~006) — 이전 회차 제목·내용을 복사한 새 DRAFT 회차 */
    public static ApprovalRevisionEntity copyForResubmission(Long approvalId, int nextRevisionNo, ApprovalRevisionEntity previous) {
        return startDraft(approvalId, nextRevisionNo, previous.title, previous.content);
    }

    public boolean isDraft() {
        return status == ApprovalStatus.DRAFT;
    }

    /** 제목·내용 수정 (APR-002) — 호출 전 {@link #isDraft()} 검증은 서비스 책임 */
    public void updateDraft(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
