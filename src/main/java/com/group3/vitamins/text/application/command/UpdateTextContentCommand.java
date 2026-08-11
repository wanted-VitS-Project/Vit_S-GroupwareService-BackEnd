package com.group3.vitamins.text.application.command;

public record UpdateTextContentCommand(
        String userId,
        Long txtId,
        String content,
        int version,
        boolean overwrite,
        String role
) {
}
