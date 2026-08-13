package com.group3.vitamins.bidding.referencefile.application.result;

import java.time.LocalDateTime;
import java.util.List;

public record ReferenceFileListResult(
        List<Item> content
) {

    public record Item(
            Long referenceFileId,
            String fileName,
            String extension,
            String mimeType,
            long sizeBytes,
            String uploadStatus,
            String indexStatus,
            boolean selectable,
            LocalDateTime createdAt
    ) {
    }
}