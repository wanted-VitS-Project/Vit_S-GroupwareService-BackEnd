package com.group3.vitamins.activitylog.infrastructure.adapter;

import java.time.LocalDateTime;

public record ActivityLogRow(
        Long activityLogId,
        String action,
        Long resourceId,
        String resourceName,
        String fieldName,
        String beforeValue,
        String afterValue,
        String actorUserId,
        String actorName,
        Long blockId,
        String blockTitle,
        String blockType,
        LocalDateTime createdAt
) {
}
