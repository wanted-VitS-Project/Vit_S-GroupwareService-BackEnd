package com.group3.vitamins.approval.application.port;

import java.time.LocalDateTime;

/** {@code file_version} 조회 결과 중 결재 도메인이 필요로 하는 최소 정보 */
public record FileVersionSummary(
        Long fileVersionId,
        String uploadStatus,
        String fileName,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}
