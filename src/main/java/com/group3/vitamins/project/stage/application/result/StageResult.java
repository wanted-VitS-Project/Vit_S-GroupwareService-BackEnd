package com.group3.vitamins.project.stage.application.result;

public record StageResult(
        Long stageId,
        Long projectId,
        String name,
        int sortOrder
) {
}