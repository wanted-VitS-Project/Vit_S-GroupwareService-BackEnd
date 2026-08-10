package com.group3.vitamins.project.application.command;

import java.util.List;

/** 사업 카테고리 연결 (PRJ-007). 이미 연결된 카테고리가 섞여 있으면 409 다. */
public record LinkBusinessCategoriesCommand(
        Long projectId,
        List<Long> categoryIds,
        String requesterUserId,
        String role
) {
}
