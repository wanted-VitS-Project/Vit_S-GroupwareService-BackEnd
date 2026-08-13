package com.group3.vitamins.companydocument.application.command;

/**
 * 사내 문서 업로드 시작(§1) 커맨드.
 *
 * <p>{@code companyDocumentId} 가 null 이면 새 문서(버전 1, {@code category} 필수), 있으면 그 문서의 새 버전이다.
 * file 과 달리 블록이 없고 회사 ADMIN 권한만 본다 — {@code role} 로 판정한다.
 */
public record StartCompanyDocumentUploadCommand(
        String category,
        String originalFileName,
        long sizeBytes,
        String mimeType,
        String name,
        Long companyDocumentId,
        String comment,
        String requesterUserId,
        String role
) {
}
