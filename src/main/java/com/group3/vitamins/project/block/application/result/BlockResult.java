package com.group3.vitamins.project.block.application.result;

import java.time.LocalDateTime;

public record BlockResult(
        Long blockId,
        Long stepId,
        Long projectId,
        String type,
        String title,
        BlockOwner owner,
        int rowIndex,
        int sortOrder,
        int colSpan,
        LocalDateTime createdAt
) {
}