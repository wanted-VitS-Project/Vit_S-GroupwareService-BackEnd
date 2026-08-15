package com.group3.vitamins.bidding.bidnotice.presentation.api.response;

import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeAttachmentUploadStartResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record BidNoticeAttachmentUploadStartResponse(

        @Schema(description = "생성된 첨부 ID. 완료 통보 API에 그대로 사용", example = "501")
        Long attachmentId,

        @Schema(description = "S3 presigned PUT URL. 클라이언트가 이 URL로 파일을 직접 업로드")
        String uploadUrl,

        @Schema(description = "업로드 URL 만료 시각")
        Instant expiresAt
) {

    public static BidNoticeAttachmentUploadStartResponse from(BidNoticeAttachmentUploadStartResult result) {
        return new BidNoticeAttachmentUploadStartResponse(
                result.attachmentId(), result.uploadUrl(), result.expiresAt()
        );
    }
}
