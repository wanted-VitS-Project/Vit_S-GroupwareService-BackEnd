package com.group3.vitamins.text.application.command;

public record DeleteTextBlockCommand(
        String userId,
        Long txtId
) {
}
