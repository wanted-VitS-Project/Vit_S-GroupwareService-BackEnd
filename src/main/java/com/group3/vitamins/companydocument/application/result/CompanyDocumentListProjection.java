package com.group3.vitamins.companydocument.application.result;

import java.time.LocalDateTime;

/**
 * 사내 문서 목록(§3) MyBatis 위치기반 매핑 레코드.
 *
 * <p>⚠️ 필드 순서 = XML SELECT alias 순서. 어긋나면 값이 통째로 밀린다(positional 매핑).
 * 문서 단위 최신 완료 버전 1행 + 완료 버전 수.
 */
public record CompanyDocumentListProjection(
        Long companyDocumentId,
        String category,
        String name,
        Long latestVersionId,
        int latestVersionNo,
        int versionCount,
        String originalFileName,
        String extension,
        long sizeBytes,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime updatedAt
) {
}
