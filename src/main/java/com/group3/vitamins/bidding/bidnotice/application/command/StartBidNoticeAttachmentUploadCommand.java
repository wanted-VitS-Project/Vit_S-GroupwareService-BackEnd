package com.group3.vitamins.bidding.bidnotice.application.command;

public record StartBidNoticeAttachmentUploadCommand(
        Long noticeId,
        String fileName,
        String mimeType,
        long sizeBytes,
        String userId,
        String role
) {
}
