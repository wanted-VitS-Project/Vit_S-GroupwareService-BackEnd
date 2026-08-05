package com.group3.vitamins.project.stage.application.result;

public record StageSummary(
        Long stageId,
        String name,
        int sortOrder,
        int stepCount
) {
}