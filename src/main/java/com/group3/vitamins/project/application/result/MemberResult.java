package com.group3.vitamins.project.application.result;

public record MemberResult(
        Long memberId,
        String userId,
        String name,
        String permission
) {}