package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 파일 버전 조회 projection (MyBatis 매핑 대상). 버전 이력(§8)·목록(§3)이 공유하는 원시 행이다.
 * 파생값(latest·previewable)은 서비스가 계산한다.
 */
public record FileVersionProjection(
        Long fileVersionId,
        int versionNo,
        String originalFileName,
        String extension,
        long sizeBytes,
        Integer pageCount,
        String comment,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime completedAt
) {
}
