package com.group3.vitamins.text.application.command;

public record UpdateTextContentCommand(
        String userId,
        Long txtId,
        String content,
        String role
) {
}
