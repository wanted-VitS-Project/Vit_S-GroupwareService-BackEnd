package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 버전 단건 조회(§11) 결과 — 결재가 고정한 fileVersionId 로 그 버전을 연다.
 * {@code latest=false} 면 결재 이후 새 버전이 올라온 것(경고 배지). 문서가 휴지통이어도 반환한다({@code fileDeleted}).
 */
public record FileVersionSingleResult(
        Long fileVersionId,
        Long fileId,
        String fileName,
        int versionNo,
        boolean latest,
        int latestVersionNo,
        String originalFileName,
        String extension,
        long sizeBytes,
        Integer pageCount,
        boolean previewable,
        String comment,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime completedAt,
        boolean fileDeleted
) {
}
