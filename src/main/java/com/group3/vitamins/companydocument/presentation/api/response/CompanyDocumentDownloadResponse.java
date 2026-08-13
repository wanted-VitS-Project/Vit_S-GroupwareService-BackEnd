package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentDownloadResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "다운로드 URL 발급 응답. downloadUrl 로 저장소에서 직접 받는다.")
public record CompanyDocumentDownloadResponse(
        @Schema(description = "버전 ID") Long versionId,
        @Schema(description = "원본 파일명") String originalFileName,
        @Schema(description = "크기(바이트)") long sizeBytes,
        @Schema(description = "presigned GET URL") String downloadUrl,
        @Schema(description = "URL 만료 시각(5분)", example = "2026-08-13T00:05:00Z") Instant expiresAt
) {

    public static CompanyDocumentDownloadResponse from(CompanyDocumentDownloadResult r) {
        return new CompanyDocumentDownloadResponse(
                r.versionId(), r.originalFileName(), r.sizeBytes(), r.downloadUrl(), r.expiresAt());
    }
}
