package com.group3.vitamins.bidding.referencefile.application.result;

import java.time.LocalDateTime;

public record StartReferenceFileUploadResult(
        Long referenceFileId,
        String uploadUrl,
        LocalDateTime expiresAt
) {
}