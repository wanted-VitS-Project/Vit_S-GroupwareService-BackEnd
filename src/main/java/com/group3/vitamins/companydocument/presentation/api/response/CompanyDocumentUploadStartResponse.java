package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentUploadStartResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "업로드 시작 응답. uploadUrl 로 저장소에 직접 PUT 한 뒤 완료 통보를 호출한다.")
public record CompanyDocumentUploadStartResponse(
        @Schema(description = "문서 번호", example = "12")
        Long companyDocumentId,

        @Schema(description = "생성된 버전 번호(UPLOADING)", example = "34")
        Long versionId,

        @Schema(description = "버전 차수", example = "1")
        int versionNo,

        @Schema(description = "presigned PUT URL", example = "https://example-bucket.s3.ap-northeast-2.amazonaws.com/...")
        String uploadUrl,

        @Schema(description = "업로드 URL 만료 시각(10분)", example = "2026-08-13T00:10:00Z")
        Instant expiresAt
) {

    public static CompanyDocumentUploadStartResponse from(CompanyDocumentUploadStartResult r) {
        return new CompanyDocumentUploadStartResponse(
                r.companyDocumentId(), r.versionId(), r.versionNo(), r.uploadUrl(), r.expiresAt());
    }
}
