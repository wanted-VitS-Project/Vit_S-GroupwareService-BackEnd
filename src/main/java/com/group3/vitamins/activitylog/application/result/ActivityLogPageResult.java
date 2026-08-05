package com.group3.vitamins.activitylog.application.result;

import java.util.List;

public record ActivityLogPageResult(
        List<ActivityLogResult> activities,
        Long nextCursor,
        boolean hasNext) {
}
