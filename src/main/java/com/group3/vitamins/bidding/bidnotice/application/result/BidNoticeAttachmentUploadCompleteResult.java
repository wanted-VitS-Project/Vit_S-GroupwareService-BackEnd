package com.group3.vitamins.bidding.bidnotice.application.result;

public record BidNoticeAttachmentUploadCompleteResult(
        Long attachmentId,
        String fileName,
        long sizeBytes
) {
}
