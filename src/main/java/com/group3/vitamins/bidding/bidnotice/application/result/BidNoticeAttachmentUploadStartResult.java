package com.group3.vitamins.bidding.bidnotice.application.result;

import java.time.Instant;

public record BidNoticeAttachmentUploadStartResult(
        Long attachmentId,
        String uploadUrl,
        Instant expiresAt
) {
}
