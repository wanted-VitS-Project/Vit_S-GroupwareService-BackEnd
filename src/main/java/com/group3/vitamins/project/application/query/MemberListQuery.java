package com.group3.vitamins.project.application.query;

public record MemberListQuery(
        Long projectId,
        String requesterUserId,
        String role
) {
}