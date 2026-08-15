package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.bidnotice.application.command.StartBidNoticeAttachmentUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StartBidNoticeAttachmentUploadRequest(

        @NotBlank @Size(max = 255)
        @Schema(description = "업로드할 파일의 원본 파일명", example = "제안요청서.pdf")
        String fileName,

        @NotBlank @Size(max = 100)
        @Schema(description = "파일 MIME 타입", example = "application/pdf")
        String mimeType,

        @Positive
        @Schema(description = "파일 크기(바이트). 최대 50MB", example = "1048576")
        long sizeBytes
) {

    public StartBidNoticeAttachmentUploadCommand toCommand(Long noticeId, String userId, String role) {
        return new StartBidNoticeAttachmentUploadCommand(noticeId, fileName, mimeType, sizeBytes, userId, role);
    }
}
