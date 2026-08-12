package com.group3.vitamins.bidding.referencefile.application.result;

import java.time.LocalDateTime;

public record CompleteReferenceFileUploadResult(
        Long referenceFileId,
        String fileName,
        String uploadStatus,
        String indexStatus,
        LocalDateTime completedAt
) {
}