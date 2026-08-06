package com.group3.vitamins.image.application.query;

public record GetImageDownloadQuery(
        String userId,
        Long imgBlockId,
        Long imgId,
        String role
) {
}
