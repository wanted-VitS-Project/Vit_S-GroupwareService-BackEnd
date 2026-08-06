package com.group3.vitamins.image.application.query;

public record GetProjectImagesQuery(
        String userId,
        Long projectId,
        String role
) {
}
