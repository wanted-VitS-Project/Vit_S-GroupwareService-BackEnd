package com.group3.vitamins.activitylog.application.result;

public record StepAccessResult(
        Long stepId,
        Long projectId,
        String permission
) {
}
