package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 프로젝트 전체 파일 모아보기(§12) projection (MyBatis 매핑 대상).
 * 프로젝트 문서함 read model 의 원시 행이다 — 문서 1건 + 그 문서의 최신 완료 버전 + 전체 버전 수 + 위치(스텝·블록).
 * 블록이 soft delete 된 고아 파일이면 {@code blockId}·{@code blockTitle} 이 {@code null} 이고 {@code blockDeleted=true} 이며,
 * {@code stepId}·{@code stepName} 은 삭제된 블록에 남은 {@code step_id} 로 해석한다.
 * 파생값(previewable)은 서비스가 계산한다.
 */
public record ProjectFileProjection(
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
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime updatedAt
) {
}
