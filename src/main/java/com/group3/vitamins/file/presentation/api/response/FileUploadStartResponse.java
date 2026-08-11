package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FileUploadStartResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "업로드 시작 응답. uploadUrl 로 저장소에 직접 PUT 한 뒤 완료 통보를 호출한다.")
public record FileUploadStartResponse(
        @Schema(description = "문서 번호", example = "31")
        Long fileId,

        @Schema(description = "생성된 버전 번호(UPLOADING)", example = "74")
        Long fileVersionId,

        @Schema(description = "버전 차수", example = "2")
        int versionNo,

        @Schema(description = "presigned PUT URL", example = "https://vitamins-dev-files.s3.ap-northeast-2.amazonaws.com/...")
        String uploadUrl,

        @Schema(description = "업로드 URL 만료 시각(10분)", example = "2026-08-06T00:10:00Z")
        Instant expiresAt
) {

    public static FileUploadStartResponse from(FileUploadStartResult result) {
        return new FileUploadStartResponse(
                result.fileId(),
                result.fileVersionId(),
                result.versionNo(),
                result.uploadUrl(),
                result.expiresAt()
        );
    }
}
