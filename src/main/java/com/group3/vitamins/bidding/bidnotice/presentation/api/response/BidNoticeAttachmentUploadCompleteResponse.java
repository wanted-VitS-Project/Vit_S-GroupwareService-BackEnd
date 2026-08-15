package com.group3.vitamins.bidding.bidnotice.presentation.api.response;

import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeAttachmentUploadCompleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record BidNoticeAttachmentUploadCompleteResponse(

        @Schema(description = "첨부 ID", example = "501")
        Long attachmentId,

        @Schema(description = "원본 파일명", example = "제안요청서.pdf")
        String fileName,

        @Schema(description = "저장소에서 검증된 실제 파일 크기(바이트)", example = "1048576")
        long sizeBytes
) {

    public static BidNoticeAttachmentUploadCompleteResponse from(BidNoticeAttachmentUploadCompleteResult result) {
        return new BidNoticeAttachmentUploadCompleteResponse(
                result.attachmentId(), result.fileName(), result.sizeBytes()
        );
    }
}
