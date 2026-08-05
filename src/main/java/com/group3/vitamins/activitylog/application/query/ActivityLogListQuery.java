package com.group3.vitamins.activitylog.application.query;

public record ActivityLogListQuery(
        Long stepId,
        Long blockId,
        Long cursor,
        Integer size,
        String requesterUserId,
        String role
) {
}
