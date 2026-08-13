package com.group3.vitamins.companydocument.application.result;

import java.time.LocalDateTime;

/**
 * 사내 문서 목록(§3) 항목 결과. {@link CompanyDocumentListProjection} + 서비스 파생값 {@code previewable}(PDF 만 true).
 */
public record CompanyDocumentListItemResult(
        Long companyDocumentId,
        String category,
        String name,
        Long latestVersionId,
        int latestVersionNo,
        int versionCount,
        String originalFileName,
        String extension,
        long sizeBytes,
        boolean previewable,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime updatedAt
) {

    public static CompanyDocumentListItemResult from(CompanyDocumentListProjection p, boolean previewable) {
        return new CompanyDocumentListItemResult(
                p.companyDocumentId(), p.category(), p.name(),
                p.latestVersionId(), p.latestVersionNo(), p.versionCount(),
                p.originalFileName(), p.extension(), p.sizeBytes(), previewable,
                p.uploaderName(), p.uploaderDepartment(), p.uploaderPosition(), p.updatedAt());
    }
}
