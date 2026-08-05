package com.group3.vitamins.activitylog.application.result;

import java.time.LocalDateTime;

public record ActivityLogResult(
        Long activityLogId,
        String action,
        String fieldName,
        String beforeValue,
        String afterValue,
        Long resourceId,
        Actor actor,
        Block block,
        LocalDateTime createdAt
) {

    public record Actor(
            String userId,
            String name,
            String profileImageUrl
    ) {
    }

    public record Block(
            Long blockId,
            String title,
            String type
    ) {
    }
}
