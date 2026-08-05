package com.group3.vitamins.activitylog.application.port;

import com.group3.vitamins.activitylog.application.result.ActivityLogLookupResult;
import com.group3.vitamins.activitylog.application.result.BlockStepResult;
import com.group3.vitamins.activitylog.application.result.StepAccessResult;

import java.util.List;
import java.util.Optional;

public interface ActivityLogQueryPort {

    Optional<StepAccessResult> findStepAccess(Long stepId, String userId);

    Optional<BlockStepResult> findBlockStep(Long blockId);

    List<ActivityLogLookupResult> findActivityLogs(Long stepId, Long blockId, Long cursor, int limit);
}
