package com.group3.vitamins.activitylog.application.port;

import com.group3.vitamins.activitylog.application.result.ActivityLogLookupResult;
import com.group3.vitamins.activitylog.application.result.BlockStepResult;
import com.group3.vitamins.activitylog.application.result.StepAccessResult;

import java.util.List;
import java.util.Optional;

public interface ActivityLogQueryPort {

    Optional<StepAccessResult> findStepAccess(Long stepId, String userId, Long companyId);

    Optional<BlockStepResult> findBlockStep(Long blockId, Long companyId);

    List<ActivityLogLookupResult> findActivityLogs(
            Long stepId,
            Long blockId,
            Long cursor,
            int limit,
            Long companyId
    );
}
