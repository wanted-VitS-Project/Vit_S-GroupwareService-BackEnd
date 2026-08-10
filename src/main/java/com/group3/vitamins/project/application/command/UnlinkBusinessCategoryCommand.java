package com.group3.vitamins.project.application.command;

/** 사업 카테고리 해제. 연결 행이 없으면 404 다. */
public record UnlinkBusinessCategoryCommand(
        Long projectId,
        Long categoryId,
        String requesterUserId,
        String role
) {
}
