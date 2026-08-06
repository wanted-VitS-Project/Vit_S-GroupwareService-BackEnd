package com.group3.vitamins.file.application.result;

import java.time.Instant;

/** 다운로드 URL 발급(§9) 결과. 클라이언트가 downloadUrl 로 저장소에서 직접 받는다. */
public record DownloadUrlResult(
        Long fileVersionId,
        String originalFileName,
        long sizeBytes,
        String downloadUrl,
        Instant expiresAt
) {
}
