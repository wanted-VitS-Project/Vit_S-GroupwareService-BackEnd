package com.group3.vitamins.image.application.command;

public record DeleteImageItemCommand(
        String userId,
        Long imgId
) {
}
