package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FileViewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "파일 관리 화면(FILE-Q) 항목. 문서 단위 최신 완료 버전 1행 + 프로젝트·스텝·블록 위치. "
        + "블록이 삭제된 고아 파일이면 blockId·blockTitle 이 null 이고 blockDeleted=true.")
public record FileViewResponse(
        @Schema(description = "스텝 번호") Long stepId,
        @Schema(description = "스텝 표시명") String stepName,
        @Schema(description = "블록 번호. 고아 파일이면 null") Long blockId,
        @Schema(description = "블록 제목. 고아 파일이면 null") String blockTitle,
        @Schema(description = "원래 블록이 삭제됐는지(고아 파일)") boolean blockDeleted,
        @Schema(description = "프로젝트 번호") Long projectId,
        @Schema(description = "프로젝트명") String projectName,
        @Schema(description = "문서 번호") Long fileId,
        @Schema(description = "문서 표시명") String name,
        @Schema(description = "최신 버전 ID") Long latestVersionId,
        @Schema(description = "최신 버전 차수") int latestVersionNo,
        @Schema(description = "전체 버전 수") int versionCount,
        @Schema(description = "최신 버전 원본 파일명") String originalFileName,
        @Schema(description = "확장자") String extension,
        @Schema(description = "최신 버전 크기(바이트)") long sizeBytes,
        @Schema(description = "미리보기 가능 여부(PDF 만 true)") boolean previewable,
        @Schema(description = "업로더(스냅샷)") String uploaderName,
        @Schema(description = "업로더 부서(스냅샷·null 허용)") String uploaderDepartment,
        @Schema(description = "업로더 직급(스냅샷·null 허용)") String uploaderPosition,
        @Schema(description = "최신 버전 업로드 시각") String updatedAt
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static FileViewResponse from(FileViewResult r) {
        return new FileViewResponse(
                r.stepId(), r.stepName(), r.blockId(), r.blockTitle(), r.blockDeleted(),
                r.projectId(), r.projectName(), r.fileId(), r.name(),
                r.latestVersionId(), r.latestVersionNo(), r.versionCount(),
                r.originalFileName(), r.extension(), r.sizeBytes(), r.previewable(),
                r.uploaderName(), r.uploaderDepartment(), r.uploaderPosition(),
                r.updatedAt() == null ? null : r.updatedAt().format(FMT));
    }
}
