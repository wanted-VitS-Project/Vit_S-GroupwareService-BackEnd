package com.group3.vitamins.bidding.referencefile.presentation.api.response;

import com.group3.vitamins.bidding.referencefile.application.result.StartReferenceFileUploadResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record StartReferenceFileUploadResponse(

        @Schema(description = "기준자료 ID", example = "501")
        Long referenceFileId,

        @Schema(description = "10분 유효한 presigned 업로드 URL")
        String uploadUrl,

        @Schema(description = "업로드 URL 만료 시각")
        LocalDateTime expiresAt
) {

    public static StartReferenceFileUploadResponse from(StartReferenceFileUploadResult result) {
        return new StartReferenceFileUploadResponse(
                result.referenceFileId(), result.uploadUrl(), result.expiresAt()
        );
    }
}