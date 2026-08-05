package com.group3.vitamins.approval.application.result;

import java.time.LocalDateTime;

/** 결재 문서 1건 + 라이브 조회한 파일 정보 — 응답 조립용 */
public record ApprovalDocumentView(
        Long documentId,
        Long fileVersionId,
        String fileName,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}
