package com.group3.vitamins.project.infrastructure.adapter;

public record ProjectMemberRow(
        Long memberId,
        String userId,
        String name,
        String department,
        String permission,
        boolean resigned
) {
}