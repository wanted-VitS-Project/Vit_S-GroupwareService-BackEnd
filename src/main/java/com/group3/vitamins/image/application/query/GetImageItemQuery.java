package com.group3.vitamins.image.application.query;

public record GetImageItemQuery(
        String userId,
        Long imgBlockId,
        int currentOrderIndex,
        ImageNavigationDirection direction,
        String role
) {
}
