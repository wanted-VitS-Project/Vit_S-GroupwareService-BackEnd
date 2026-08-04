package com.group3.vitamins.project.step.application.command;

import java.time.LocalDate;

public record CreateStepCommand(
        Long projectId,
        Long stageId,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String ownerUserId,
        String requesterUserId,
        String role
) {
}