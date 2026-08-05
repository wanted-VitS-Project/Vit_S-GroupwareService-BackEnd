package com.group3.vitamins.image.application.command;

import java.util.List;

public record UpdateImageItemsCommand(
        String userId,
        Long imgBlockId,
        List<Entry> images
) {
    public record Entry(Long imgId, String caption) {
    }
}
