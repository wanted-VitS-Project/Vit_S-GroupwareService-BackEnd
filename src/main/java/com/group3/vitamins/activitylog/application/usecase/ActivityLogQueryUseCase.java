package com.group3.vitamins.activitylog.application.usecase;

import com.group3.vitamins.activitylog.application.query.ActivityLogListQuery;
import com.group3.vitamins.activitylog.application.result.ActivityLogPageResult;

public interface ActivityLogQueryUseCase {
    ActivityLogPageResult getActivityLogs(ActivityLogListQuery query);
}
