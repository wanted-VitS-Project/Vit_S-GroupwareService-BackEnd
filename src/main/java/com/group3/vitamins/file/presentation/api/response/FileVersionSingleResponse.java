package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FileVersionSingleResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "버전 단건 조회 응답(§11 · 결재용). latest=false 면 결재 이후 새 버전 있음.")
public record FileVersionSingleResponse(
        Long fileVersionId,
        Long fileId,
        String fileName,
        int versionNo,
        @Schema(description = "최신 버전인지. false 면 경고 배지", example = "false") boolean latest,
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
        String completedAt,
        @Schema(description = "문서가 휴지통에 있는지", example = "false") boolean fileDeleted
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static FileVersionSingleResponse from(FileVersionSingleResult r) {
        return new FileVersionSingleResponse(
                r.fileVersionId(), r.fileId(), r.fileName(), r.versionNo(), r.latest(), r.latestVersionNo(),
                r.originalFileName(), r.extension(), r.sizeBytes(), r.pageCount(), r.previewable(), r.comment(),
                r.uploaderName(), r.uploaderDepartment(), r.uploaderPosition(),
                r.completedAt() == null ? null : r.completedAt().format(FMT), r.fileDeleted());
    }
}
