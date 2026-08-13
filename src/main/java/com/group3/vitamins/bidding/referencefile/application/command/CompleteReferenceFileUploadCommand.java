package com.group3.vitamins.bidding.referencefile.application.command;

public record CompleteReferenceFileUploadCommand(
        Long referenceFileId,
        String userId,
        String role
) {
}