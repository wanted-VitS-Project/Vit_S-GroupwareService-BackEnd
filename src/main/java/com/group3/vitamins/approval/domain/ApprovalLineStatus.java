package com.group3.vitamins.approval.domain;

/**
 * {@code approval_line} 결재선 상태 (`APR-V1.md` §2-A·§2-C).
 *
 * <p>SUB-002: 상신 시 1번 순번만 {@code ACTIVE}, 나머지는 {@code WAITING}.
 * PRC-002: 승인 시 다음 순번이 {@code ACTIVE} 로, 마지막 순번이면 전체 완료.
 * PRC-007: 반려 시 이후 {@code WAITING} 단계는 전부 {@code CANCELED}.
 */
public enum ApprovalLineStatus {
    DRAFT,
    WAITING,
    ACTIVE,
    APPROVED,
    REJECTED,
    CANCELED
}
