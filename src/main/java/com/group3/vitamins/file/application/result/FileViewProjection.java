package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 파일 관리 화면(전사 파일 관리 · 내 프로젝트 파일 모아보기 · FILE-Q) projection.
 *
 * <p>여러 프로젝트에 걸친 파일 행 — 문서 1건 + 그 문서의 최신 완료 버전 + 위치(프로젝트·스텝·블록).
 * 블록이 soft delete 된 고아 파일이면 {@code blockId}·{@code blockTitle} 이 null 이고 {@code blockDeleted=true}.
 * ⚠️ 필드 순서 = XML SELECT alias 순서(MyBatis 위치 기반 생성자 매핑).
 */
public record FileViewProjection(
        Long stepId,
        String stepName,
        Long blockId,
        String blockTitle,
        boolean blockDeleted,
        Long projectId,
        String projectName,
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
