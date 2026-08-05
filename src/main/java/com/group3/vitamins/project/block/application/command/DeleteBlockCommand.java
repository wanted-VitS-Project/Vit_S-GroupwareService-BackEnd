package com.group3.vitamins.project.block.application.command;

public record DeleteBlockCommand(
        Long blockId,
        String requesterUserId,
        String role
) {
}