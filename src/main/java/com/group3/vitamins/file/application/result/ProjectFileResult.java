package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 프로젝트 전체 파일 모아보기(§12) 결과 항목. 문서 단위 최신 완료 버전 1행 + 위치(스텝·블록).
 * previewable 은 서비스가 계산한 파생값이다. 고아 파일이면 blockId·blockTitle 이 null 이고 blockDeleted=true.
 */
public record ProjectFileResult(
        Long stepId,
        String stepName,
        Long blockId,
        String blockTitle,
        boolean blockDeleted,
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
        LocalDateTime updatedAt
) {
}
