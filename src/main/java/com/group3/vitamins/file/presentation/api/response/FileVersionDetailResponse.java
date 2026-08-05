package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FileVersionDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "파일 버전 상세 — 완료 통보(§2) 응답. 버전 이력·단건에서도 같은 형태를 쓴다.")
public record FileVersionDetailResponse(
        Long fileId,
        Long fileVersionId,
        int versionNo,
        @Schema(description = "문서 표시명", example = "제안서") String name,
        @Schema(description = "원본 파일명", example = "제안서_v2.pdf") String originalFileName,
        String extension,
        long sizeBytes,
        @Schema(description = "PDF 총 페이지 수. PDF 가 아니거나 추출 실패면 null", example = "42") Integer pageCount,
        String comment,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        @Schema(description = "업로드 완료 시각 yyyy-MM-dd HH:mm:ss", example = "2026-08-06 00:53:02") String completedAt
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static FileVersionDetailResponse from(FileVersionDetailResult r) {
        return new FileVersionDetailResponse(
                r.fileId(), r.fileVersionId(), r.versionNo(), r.name(),
                r.originalFileName(), r.extension(), r.sizeBytes(), r.pageCount(), r.comment(),
                r.uploaderName(), r.uploaderDepartment(), r.uploaderPosition(),
                r.completedAt() == null ? null : r.completedAt().format(FMT));
    }
}
