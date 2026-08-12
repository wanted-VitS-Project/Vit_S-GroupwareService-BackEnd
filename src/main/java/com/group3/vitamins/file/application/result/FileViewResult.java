package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 파일 관리 화면(FILE-Q) 결과 항목. {@link FileViewProjection} + 서비스 파생값 {@code previewable}(PDF 만 true).
 * 전사 파일 관리는 페이지로, 내 프로젝트 파일은 리스트로 감싸 내린다.
 */
public record FileViewResult(
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
        boolean previewable,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime updatedAt
) {

    public static FileViewResult from(FileViewProjection p, boolean previewable) {
        return new FileViewResult(
                p.stepId(), p.stepName(), p.blockId(), p.blockTitle(), p.blockDeleted(),
                p.projectId(), p.projectName(), p.fileId(), p.name(),
                p.latestVersionId(), p.latestVersionNo(), p.versionCount(),
                p.originalFileName(), p.extension(), p.sizeBytes(), previewable,
                p.uploaderName(), p.uploaderDepartment(), p.uploaderPosition(), p.updatedAt());
    }
}
