package com.group3.vitamins.approval.domain.model;

/** APR-001: {@code approval} + 1회차 {@code approval_revision} 을 한 트랜잭션으로 만든 결과 */
public record ApprovalWithRevision(Approval approval, ApprovalRevision revision) {
}
