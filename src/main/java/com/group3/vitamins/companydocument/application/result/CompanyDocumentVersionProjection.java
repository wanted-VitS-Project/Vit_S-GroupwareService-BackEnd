package com.group3.vitamins.companydocument.application.result;

import java.time.LocalDateTime;

/**
 * 사내 문서 버전 이력(§7) MyBatis 위치기반 매핑 레코드.
 *
 * <p>⚠️ 필드 순서 = XML SELECT alias 순서. 완료 버전만, 차수 내림차순.
 */
public record CompanyDocumentVersionProjection(
        Long versionId,
        int versionNo,
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
