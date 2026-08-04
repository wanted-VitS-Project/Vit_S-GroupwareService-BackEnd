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
 * 결재. 팀 ERD 의 {@code approval} 테이블 (`APR-V1.md` §2-A).
 *
 * <p>⚠️ {@code approval_id}(PK) 와 {@code block_id} 는 서로 다른 값이다(INV-10).
 * {@code block_id} 는 FK 제약이 없는 UNIQUE 컬럼이고, 블록팀이 이미 만든 {@code block}
 * 행을 가리킬 뿐이다. <b>{@code Block} 과 JPA 연관관계를 맺지 않는다</b> — 도메인 간
 * 엔티티 결합을 막기 위함(INV-08). 블록 정보가 필요하면 별도 조회로 가져온다.
 */
@Entity
@Table(name = "approval")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    /** 결재 블록의 {@code block_id}. FK 아님 — 위 클래스 주석 참고 */
    @Column(name = "block_id")
    private Long blockId;

    /** 기안자 사번 */
    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "enum('DRAFT','IN_PROGRESS','REJECTED','COMPLETED','CANCELED')")
    private ApprovalStatus status;

    /** 현재 상신 회차 (`approval_revision.revision_no`) */
    @Column(name = "current_revision_no", nullable = false)
    private int currentRevisionNo;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 결재 상세 생성 (APR-001) — 이미 존재하는 {@code blockId} 에 1회차 DRAFT 로 시작한다 */
    public static ApprovalEntity draftFor(Long blockId, String drafterId) {
        ApprovalEntity approval = new ApprovalEntity();
        approval.blockId = blockId;
        approval.userId = drafterId;
        approval.status = ApprovalStatus.DRAFT;
        approval.currentRevisionNo = 1;
        return approval;
    }

    public boolean isInProgress() {
        return status == ApprovalStatus.IN_PROGRESS;
    }

    public boolean isRejected() {
        return status == ApprovalStatus.REJECTED;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
