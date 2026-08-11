package com.group3.vitamins.project.application.result;

public record ProjectProgressResult(
        Long projectId,
        int totalStepCount,
        int doneStepCount,
        Integer progressRate
) {
}