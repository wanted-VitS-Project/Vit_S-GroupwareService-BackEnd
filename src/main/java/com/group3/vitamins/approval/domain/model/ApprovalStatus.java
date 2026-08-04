package com.group3.vitamins.approval.domain.model;

/**
 * {@code approval}·{@code approval_revision} 공용 상태 (`APR-V1.md` §2-A·§2-B).
 *
 * <p>두 테이블이 같은 5종 상태를 쓴다 — SUB-002 상신 시 회차와 결재 전체가 함께
 * {@code IN_PROGRESS} 로 바뀌고, PRC-002 최종 승인 시 함께 {@code COMPLETED} 로 바뀐다.
 */
public enum ApprovalStatus {
    DRAFT,
    IN_PROGRESS,
    REJECTED,
    COMPLETED,
    CANCELED
}
