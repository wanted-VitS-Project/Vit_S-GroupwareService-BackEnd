package com.group3.vitamins.activitylog.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActivityLogResult(
        Long activityLogId,
        String action,
        String targetType,
        String displayName,
        String fieldName,
        String beforeValue,
        String afterValue,
        Resource resource,
        Actor actor,
        Block block,
        LocalDateTime createdAt
) {

    public static ActivityLogResult from(ActivityLogLookupResult result) {
        String targetType = result.resourceId() == null ? "BLOCK" : "RESOURCE";
        String displayName = result.resourceName() != null ? result.resourceName() : result.blockTitle();

        return new ActivityLogResult(
                result.activityLogId(),
                result.action(),
                targetType,
                displayName,
                result.fieldName(),
                result.beforeValue(),
                result.afterValue(),
                new Resource(result.resourceId(), result.resourceName()),
                new Actor(result.actorUserId(), result.actorName(), result.actorResignedAt()),
                new Block(result.blockId(), result.blockTitle(), result.blockType()),
                result.createdAt()
        );
    }

    public record Resource(
            Long resourceId,
            String name
    ) {
    }

    public record Actor(
            String userId,
            String name,
            LocalDate resignedAt
    ) {
    }

    public record Block(
            Long blockId,
            String title,
            String type
    ) {
    }
}
