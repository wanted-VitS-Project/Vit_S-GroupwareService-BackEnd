package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;
import java.util.List;

/** 버전 이력 조회(§8) 결과. */
public record VersionHistoryResult(
        Long fileId,
        String name,
        int versionCount,
        List<Item> content
) {

    /** 버전 이력 항목. latest·previewable 은 서비스가 계산한 파생값. */
    public record Item(
            Long fileVersionId,
            int versionNo,
            boolean latest,
            String originalFileName,
            String extension,
            long sizeBytes,
            Integer pageCount,
            boolean previewable,
            String comment,
            String uploaderName,
            String uploaderDepartment,
            String uploaderPosition,
            LocalDateTime completedAt
    ) {
    }
}
