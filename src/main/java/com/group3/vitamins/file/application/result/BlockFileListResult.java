package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;
import java.util.List;

/** 블록 파일 목록(§3) 결과. canEdit 로 프론트가 업로드·삭제 버튼 노출을 정한다. */
public record BlockFileListResult(
        Long blockId,
        boolean canEdit,
        List<Item> content
) {

    public record Item(
            Long fileId,
            String name,
            Long latestVersionId,
            int latestVersionNo,
            int versionCount,
            String originalFileName,
            String extension,
            long sizeBytes,
            boolean previewable,
            String uploaderName,
            String uploaderDepartment,
            String uploaderPosition,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
    }
}
