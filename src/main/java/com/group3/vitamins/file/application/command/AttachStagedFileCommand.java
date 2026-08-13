package com.group3.vitamins.file.application.command;

/**
 * 입찰 검토 파일 귀속 커맨드 (FILE-V1 §2-G-1 · 확정 계약).
 *
 * <p>임시 S3 객체(temporaryStorageKey)를 정식 프로젝트 파일로 귀속한다. 입찰 도메인이 in-process 로 호출한다.
 * 업로더 스냅샷은 넘기지 않는다 — {@code requesterUserId} 로 파일 도메인이 조회한다(PROMOTE-005).
 *
 * @param comment            관례상 "AI 검토 첨부"
 * @param allowDuplicateName 항상 true — 귀속은 늘 새 문서로 만든다(PROMOTE-009). 계약 명시용 필드
 * @param idempotencyKey     {@code bidReviewDocumentId}. 재시도 중복 귀속 방지(PROMOTE-007)
 */
public record AttachStagedFileCommand(
        long companyId,
        long projectId,
        String requesterUserId,
        String temporaryStorageKey,
        String originalFileName,
        long sizeBytes,
        String checksum,
        String name,
        String comment,
        boolean allowDuplicateName,
        String idempotencyKey) {
}
