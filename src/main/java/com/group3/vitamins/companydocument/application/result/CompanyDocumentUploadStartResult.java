package com.group3.vitamins.companydocument.application.result;

import java.time.Instant;

/** 사내 문서 업로드 시작(§1) 결과 — presigned PUT URL 발급. */
public record CompanyDocumentUploadStartResult(
        Long companyDocumentId,
        Long versionId,
        int versionNo,
        String uploadUrl,
        Instant expiresAt
) {
}
