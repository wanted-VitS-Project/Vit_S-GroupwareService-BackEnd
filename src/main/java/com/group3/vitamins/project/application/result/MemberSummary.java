package com.group3.vitamins.project.application.result;

public record MemberSummary(
        Long memberId,
        String userId,
        String name,
        String department,
        String permission,
        boolean resigned
) {
}