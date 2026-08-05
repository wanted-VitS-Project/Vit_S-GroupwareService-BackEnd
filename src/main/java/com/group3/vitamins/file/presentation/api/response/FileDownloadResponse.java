package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.DownloadUrlResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "다운로드 URL 발급 응답. downloadUrl 로 저장소에서 직접 받는다(만료 5분).")
public record FileDownloadResponse(
        Long fileVersionId,
        String originalFileName,
        long sizeBytes,
        String downloadUrl,
        @Schema(description = "다운로드 URL 만료 시각", example = "2026-08-06T00:05:00Z") Instant expiresAt
) {

    public static FileDownloadResponse from(DownloadUrlResult r) {
        return new FileDownloadResponse(
                r.fileVersionId(), r.originalFileName(), r.sizeBytes(), r.downloadUrl(), r.expiresAt());
    }
}
