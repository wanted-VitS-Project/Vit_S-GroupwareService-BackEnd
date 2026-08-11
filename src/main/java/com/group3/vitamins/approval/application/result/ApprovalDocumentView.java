package com.group3.vitamins.approval.application.result;

import java.time.LocalDateTime;

/**
 * 결재 문서 1건 + 라이브 조회한 파일 정보 — 응답 조립용.
 *
 * @param fileDeleted 원본 문서가 휴지통에 있으면 {@code true}. 이름·크기는 그대로 두고 상태만
 *                    노출한다(`DELETE.md` D-6 · DEL-010) — 완료된 결재의 첨부도 휴지통으로
 *                    옮겨질 수 있어서, 표시가 없으면 사용자가 정상 첨부로 믿고 열려다 실패한다.
 */
public record ApprovalDocumentView(
        Long documentId,
        Long fileVersionId,
        String fileName,
        Long fileSize,
        LocalDateTime uploadedAt,
        boolean fileDeleted
) {
}
