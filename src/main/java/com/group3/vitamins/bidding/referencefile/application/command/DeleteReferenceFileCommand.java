package com.group3.vitamins.bidding.referencefile.application.command;

public record DeleteReferenceFileCommand(
        Long referenceFileId,
        String userId,
        String role
) {
}