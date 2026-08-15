package com.group3.vitamins.bidding.bidnotice.application.command;

public record CompleteBidNoticeAttachmentUploadCommand(
        Long noticeId,
        Long attachmentId,
        String userId,
        String role
) {
}
