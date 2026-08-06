package com.group3.vitamins.file.application.result;

import java.time.Instant;

/** 업로드 시작(§1) 결과 — 클라이언트는 {@code uploadUrl} 로 저장소에 직접 PUT 한 뒤 완료 통보(§2)를 호출한다. */
public record FileUploadStartResult(
        Long fileId,
        Long fileVersionId,
        int versionNo,
        String uploadUrl,
        Instant expiresAt
) {
}
