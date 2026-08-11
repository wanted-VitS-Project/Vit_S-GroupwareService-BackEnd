package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 블록 파일 목록(§3) projection — 문서 1건 + 그 문서의 최신 완료 버전 정보 + 전체 버전 수.
 * 완료 버전이 0개인 문서는 조인에서 자연히 빠진다(§3 규칙).
 */
public record BlockFileProjection(
        Long fileId,
        String name,
        Long latestVersionId,
        int latestVersionNo,
        int versionCount,
        String originalFileName,
        String extension,
        long sizeBytes,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        // ⚠️ 맨 끝 고정. FileQueryMapper.findBlockFiles 의 SELECT 컬럼 순서와 1:1 이어야 값이 안 밀린다(§6 trap 7).
        int version
) {
}
