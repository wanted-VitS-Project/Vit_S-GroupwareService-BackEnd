package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.ProjectFileVersionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "프로젝트 파일 버전 목록 항목(§11, #138). 비타메이트 분석 선택용 — indexStatus=COMPLETED 인 버전만 프론트에서 선택 가능.")
public record ProjectFileVersionResponse(
        @Schema(description = "논리 파일 ID") Long fileId,
        @Schema(description = "문서 표시명") String name,
        @Schema(description = "분석 저장 기준 키") Long fileVersionId,
        @Schema(description = "버전 차수") int versionNo,
        @Schema(description = "최신 버전 여부(과거 버전도 목록에 포함)") boolean latest,
        @Schema(description = "원본 파일명") String originalFileName,
        @Schema(description = "확장자") String extension,
        @Schema(description = "바이트 크기") long sizeBytes,
        @Schema(description = "페이지 수(없으면 null)") Integer pageCount,
        @Schema(description = "미리보기 가능 여부") boolean previewable,
        @Schema(description = "업로드 완료 시각") String completedAt,
        @Schema(description = "인덱싱 상태(PENDING/PROCESSING/COMPLETED/FAILED)") String indexStatus
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ProjectFileVersionResponse from(ProjectFileVersionResult r) {
        return new ProjectFileVersionResponse(
                r.fileId(), r.name(), r.fileVersionId(), r.versionNo(), r.latest(),
                r.originalFileName(), r.extension(), r.sizeBytes(), r.pageCount(), r.previewable(),
                r.completedAt() == null ? null : r.completedAt().format(FMT), r.indexStatus());
    }
}
