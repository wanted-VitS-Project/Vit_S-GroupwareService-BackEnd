package com.group3.vitamins.jobposition.application.command;

public record DeleteJobPositionCommand(
        Long jobPositionId,
        String role
) {
}
