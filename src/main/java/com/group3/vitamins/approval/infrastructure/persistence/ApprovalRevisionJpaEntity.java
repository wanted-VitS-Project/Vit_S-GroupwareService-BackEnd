package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.model.ApprovalStatus;
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
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 결재 상신(회차). 팀 ERD 의 {@code approval_revision} 테이블 (`APR-V1.md` §2-A·§2-B).
 *
 * <p>INV-02: 상신({@code status != DRAFT}) 이후 이 행은 수정되지 않는다 — 재상신은 새 회차 행을 만든다.
 */
@Entity
@NoArgsConstructor
@Getter
@DynamicUpdate
@Table(name = "approval_revision")
public class ApprovalRevisionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_revision_id")
    private Long approvalRevisionId;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * DRAFT 회차 생성 — 최초 1회차(APR-001)는 {@code title=""}/{@code content=null} 로 시작하고
     * (명세상 생성 시 입력값이 없고 컬럼이 NOT NULL), 재상신(SUB-006)은 이전 회차 제목·내용을 그대로 넘긴다.
     */
    public static ApprovalRevisionJpaEntity createDraft(Long approvalId, int revisionNo, String title, String content) {
        ApprovalRevisionJpaEntity entity = new ApprovalRevisionJpaEntity();
        entity.approvalId = approvalId;
        entity.revisionNo = revisionNo;
        entity.title = title;
        entity.content = content;
        entity.status = ApprovalStatus.DRAFT;
        return entity;
    }
}
