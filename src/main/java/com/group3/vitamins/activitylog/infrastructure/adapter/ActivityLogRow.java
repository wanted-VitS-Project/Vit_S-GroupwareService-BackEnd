package com.group3.vitamins.activitylog.infrastructure.adapter;

import java.time.LocalDateTime;

public record ActivityLogRow(
        Long activityLogId,
        String action,
        Long resourceId,
        String fieldName,
        String beforeValue,
        String afterValue,
        String actorUserId,
        String actorName,
        String profileImageUrl,
        Long blockId,
        String blockTitle,
        String blockType,
        LocalDateTime createdAt
) {
}
