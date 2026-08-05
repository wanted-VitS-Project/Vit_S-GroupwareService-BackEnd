package com.group3.vitamins.project.step.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StepResult(
        Long stepId,
        Long projectId,
        Long stageId,
        String name,
        String status,
        int sortOrder,
        LocalDate startedOn,
        LocalDate endedOn,
        Owner owner,
        LocalDateTime createdAt
) {

    /** 책임자. ownerUserId 를 안 보냈으면 owner 자체가 null 이다. */
    public record Owner(String userId, String name) {
    }
}