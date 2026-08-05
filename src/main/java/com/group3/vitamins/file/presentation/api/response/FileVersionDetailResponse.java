package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FileVersionDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "파일 버전 상세 — 완료 통보(§2) 응답. 버전 이력·단건에서도 같은 형태를 쓴다.")
public record FileVersionDetailResponse(
        @Schema(description = "문서 id", example = "31") Long fileId,
        @Schema(description = "버전 id", example = "74") Long fileVersionId,
        @Schema(description = "버전 차수", example = "2") int versionNo,
        @Schema(description = "문서 표시명", example = "제안서") String name,
        @Schema(description = "원본 파일명", example = "제안서_v2.pdf") String originalFileName,
        @Schema(description = "확장자(소문자, 점 제외)", example = "pdf") String extension,
        @Schema(description = "파일 크기(byte)", example = "5242880") long sizeBytes,
        @Schema(description = "PDF 총 페이지 수. PDF 가 아니거나 추출 실패면 null", example = "42") Integer pageCount,
        @Schema(description = "버전 코멘트", example = "최종본 반영") String comment,
        @Schema(description = "업로더 이름", example = "이영희") String uploaderName,
        @Schema(description = "업로더 부서", example = "제안팀") String uploaderDepartment,
        @Schema(description = "업로더 직위", example = "선임연구원") String uploaderPosition,
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
