package com.group3.vitamins.image.application.query;

public record GetImageItemsQuery(
        String userId,
        Long imgBlockId,
        String role
) {
}
