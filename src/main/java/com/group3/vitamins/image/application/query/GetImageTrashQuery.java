package com.group3.vitamins.image.application.query;

public record GetImageTrashQuery(
        String userId,
        Long projectId,
        String role
) {
}
