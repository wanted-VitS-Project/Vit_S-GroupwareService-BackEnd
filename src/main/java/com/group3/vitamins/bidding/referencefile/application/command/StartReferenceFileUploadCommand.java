package com.group3.vitamins.bidding.referencefile.application.command;

public record StartReferenceFileUploadCommand(
        String fileName,
        String mimeType,
        long sizeBytes,
        String userId,
        String role
) {
}