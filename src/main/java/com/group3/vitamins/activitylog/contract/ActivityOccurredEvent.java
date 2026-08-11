package com.group3.vitamins.activitylog.contract;

import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.global.domain.event.DomainEvent;

import java.util.List;
import java.util.Objects;

public record ActivityOccurredEvent(
        ActivityLogAction action,
        Long blockId,
        Long resourceId,
        String resourceName,
        String actorId,
        List<ActivityFieldChange> changes
) implements DomainEvent {

    public ActivityOccurredEvent {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(blockId, "blockId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(changes, "changes must not be null");

        if (actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("changes must not be empty");
        }

        changes = List.copyOf(changes);
    }

    public static ActivityOccurredEvent of(
            ActivityLogAction action,
            Long blockId,
            Long resourceId,
            String resourceName,
            String actorId,
            List<ActivityFieldChange> changes
    ) {
        return new ActivityOccurredEvent(action, blockId, resourceId, resourceName, actorId, changes);
    }

    public static ActivityOccurredEvent of(
            ActivityLogAction action,
            Long blockId,
            Long resourceId,
            String actorId,
            List<ActivityFieldChange> changes
    ) {
        return new ActivityOccurredEvent(action, blockId, resourceId, null, actorId, changes);
    }
}
