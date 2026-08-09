package com.group3.vitamins.project.infrastructure.adapter;

/** 참여자 조회 결과 한 행. */
public record ProjectMemberRow(
        Long memberId,
        String userId,
        String name,
        String department,
        String permission,
        boolean resigned
) {
}