package com.group3.vitamins.image.application.command;

import java.util.List;

public record RestoreImageItemsCommand(
        String userId,
        List<Long> imgIds,
        String role
) {
}
