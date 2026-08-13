package com.group3.vitamins.companydocument.application.result;

import java.time.LocalDateTime;
import java.util.List;

/** 사내 문서 버전 이력(§7) 결과 — 문서 정보 + 완료 버전 목록(차수 내림차순). */
public record CompanyDocumentVersionHistoryResult(
        Long companyDocumentId,
        String name,
        String category,
        int versionCount,
        List<Item> content
) {

    public record Item(
            Long versionId,
            int versionNo,
            boolean latest,
            String originalFileName,
            String extension,
            long sizeBytes,
            Integer pageCount,
            boolean previewable,
            String comment,
            String uploaderName,
            String uploaderDepartment,
            String uploaderPosition,
            LocalDateTime completedAt
    ) {
    }
}
