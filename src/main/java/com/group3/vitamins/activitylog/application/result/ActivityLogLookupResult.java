package com.group3.vitamins.activitylog.application.result;

import java.time.LocalDateTime;

public record ActivityLogLookupResult(
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
