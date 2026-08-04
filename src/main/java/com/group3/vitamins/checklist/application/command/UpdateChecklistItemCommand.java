package com.group3.vitamins.checklist.application.command;

public record UpdateChecklistItemCommand(
        String userId,
        Long chkId,
        String content,
        Boolean changeStatusTo
) {
}
