package com.group3.vitamins.bidding.referencefile.presentation.api.response;

import com.group3.vitamins.bidding.referencefile.application.result.CompleteReferenceFileUploadResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CompleteReferenceFileUploadResponse(

        @Schema(description = "기준자료 ID", example = "501")
        Long referenceFileId,

        @Schema(description = "원본 파일명")
        String fileName,

        @Schema(description = "업로드 상태", example = "COMPLETED")
        String uploadStatus,

        @Schema(description = "인덱싱 상태", example = "PENDING")
        String indexStatus,

        @Schema(description = "업로드 완료 시각")
        LocalDateTime completedAt
) {

    public static CompleteReferenceFileUploadResponse from(CompleteReferenceFileUploadResult result) {
        return new CompleteReferenceFileUploadResponse(
                result.referenceFileId(), result.fileName(), result.uploadStatus(),
                result.indexStatus(), result.completedAt()
        );
    }
}