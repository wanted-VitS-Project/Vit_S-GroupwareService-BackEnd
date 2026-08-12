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
 * 결재. 팀 ERD 의 {@code approval} 테이블 (`APR-V1.md` §2-A, `text.infrastructure.persistence.TextJpaEntity`와 동일 구조).
 *
 * <p>⚠️ {@code approval_id}(PK) 와 {@code block_id} 는 서로 다른 값이다(INV-10). {@code block_id} 는
 * FK 제약이 없는 UNIQUE 컬럼이고, <b>{@code Block} 과 JPA 연관관계를 맺지 않는다</b>(INV-08).
 */
@Entity
@NoArgsConstructor
@Getter
@DynamicUpdate
@Table(name = "approval")
public class ApprovalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    /** 결재 블록의 {@code block_id}. FK 아님 — 위 클래스 주석 참고 */
    @Column(name = "block_id")
    private Long blockId;

    /** 기안자 사번 */
    @Column(name = "user_id", nullable = false, length = 20)
    private String drafterId;

    /** 원 기안자가 참여 불가일 때 기존 결재를 이어받은 대행 기안자. 원 기안자 감사 이력은 유지한다. */
    @Column(name = "acting_drafter_id", length = 20)
    private String actingDrafterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "enum('DRAFT','IN_PROGRESS','REJECTED','COMPLETED','CANCELED')")
    private ApprovalStatus status;

    @Column(name = "current_revision_no", nullable = false)
    private int currentRevisionNo;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 결재 상세 생성(APR-001) — 이미 존재하는 blockId 에 1회차 DRAFT 로 시작한다 */
    public static ApprovalJpaEntity createDraft(Long blockId, String drafterId) {
        ApprovalJpaEntity entity = new ApprovalJpaEntity();
        entity.blockId = blockId;
        entity.drafterId = drafterId;
        entity.status = ApprovalStatus.DRAFT;
        entity.currentRevisionNo = 1;
        return entity;
    }
}
