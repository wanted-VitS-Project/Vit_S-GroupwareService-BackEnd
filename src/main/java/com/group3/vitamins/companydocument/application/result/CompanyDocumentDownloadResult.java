package com.group3.vitamins.companydocument.application.result;

import java.time.Instant;

/** 사내 문서 다운로드 URL 발급(§8) 결과. presigned GET URL(5분). */
public record CompanyDocumentDownloadResult(
        Long versionId,
        String originalFileName,
        long sizeBytes,
        String downloadUrl,
        Instant expiresAt
) {
}
