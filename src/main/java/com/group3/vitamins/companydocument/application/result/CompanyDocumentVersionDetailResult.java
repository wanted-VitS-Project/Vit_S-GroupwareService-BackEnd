package com.group3.vitamins.companydocument.application.result;

import java.time.LocalDateTime;

/** 사내 문서 버전 상세 결과 — 완료 통보(§2) 응답 및 버전 조회 공용. 업로더 스냅샷은 nullable(§6-6). */
public record CompanyDocumentVersionDetailResult(
        Long companyDocumentId,
        Long versionId,
        int versionNo,
        String name,
        String category,
        String originalFileName,
        String extension,
        long sizeBytes,
        Integer pageCount,
        String comment,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime completedAt
) {
}
