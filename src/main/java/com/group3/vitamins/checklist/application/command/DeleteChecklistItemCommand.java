package com.group3.vitamins.checklist.application.command;

public record DeleteChecklistItemCommand(
        String userId,
        Long chkId
) {
}
