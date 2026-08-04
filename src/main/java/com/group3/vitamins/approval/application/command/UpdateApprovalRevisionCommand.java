package com.group3.vitamins.approval.application.command;

/** APR-002 — {@code title}/{@code content} 는 하나만 보내도 된다(null 이면 기존 값 유지) */
public record UpdateApprovalRevisionCommand(
        Long approvalId,
        Long revisionId,
        String title,
        String content,
        String requesterId
) {
}
