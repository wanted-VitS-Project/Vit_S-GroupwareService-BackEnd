package com.group3.vitamins.image.application.command;

import java.util.List;

public record PurgeImageItemsCommand(
        String userId,
        List<Long> imgIds,
        String role
) {
}
