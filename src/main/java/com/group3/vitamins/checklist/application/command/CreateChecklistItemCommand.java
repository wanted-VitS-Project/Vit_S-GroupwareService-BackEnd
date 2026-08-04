package com.group3.vitamins.checklist.application.command;

public record CreateChecklistItemCommand(
        String userId,
        Long chkBlockId,
        String content
) {
}
