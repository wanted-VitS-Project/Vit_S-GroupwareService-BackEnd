package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 프로젝트 파일 버전 목록(§11, #138) 결과 항목. AI/비타메이트 분석 선택용.
 * latest·previewable 은 서비스가 계산한 파생값이다.
 */
public record ProjectFileVersionResult(
        Long fileId,
        String name,
        Long fileVersionId,
        int versionNo,
        boolean latest,
        String originalFileName,
        String extension,
        long sizeBytes,
        Integer pageCount,
        boolean previewable,
        LocalDateTime completedAt,
        String indexStatus
) {
}
