package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "사내 문서 목록(§3) 항목. 문서 단위 최신 완료 버전 1행. 업로더 스냅샷은 ADMIN 업로드 시 null 일 수 있다.")
public record CompanyDocumentListItemResponse(
        @Schema(description = "문서 번호") Long companyDocumentId,
        @Schema(description = "카테고리 enum", example = "FINANCE") String category,
        @Schema(description = "문서 표시명") String name,
        @Schema(description = "최신 버전 ID") Long latestVersionId,
        @Schema(description = "최신 버전 차수") int latestVersionNo,
        @Schema(description = "전체 완료 버전 수") int versionCount,
        @Schema(description = "최신 버전 원본 파일명") String originalFileName,
        @Schema(description = "확장자") String extension,
        @Schema(description = "최신 버전 크기(바이트)") long sizeBytes,
        @Schema(description = "미리보기 가능 여부(PDF 만 true)") boolean previewable,
        @Schema(description = "업로더(스냅샷·null 허용)", nullable = true) String uploaderName,
        @Schema(description = "업로더 부서(스냅샷·null 허용)", nullable = true) String uploaderDepartment,
        @Schema(description = "업로더 직급(스냅샷·null 허용)", nullable = true) String uploaderPosition,
        @Schema(description = "최신 버전 업로드 시각 yyyy-MM-dd HH:mm:ss") String updatedAt
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CompanyDocumentListItemResponse from(CompanyDocumentListItemResult r) {
        return new CompanyDocumentListItemResponse(
                r.companyDocumentId(), r.category(), r.name(),
                r.latestVersionId(), r.latestVersionNo(), r.versionCount(),
                r.originalFileName(), r.extension(), r.sizeBytes(), r.previewable(),
                r.uploaderName(), r.uploaderDepartment(), r.uploaderPosition(),
                r.updatedAt() == null ? null : r.updatedAt().format(FMT));
    }
}
