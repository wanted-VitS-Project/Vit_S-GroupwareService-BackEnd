package com.group3.vitamins.approval.infrastructure.persistence.row;

import java.time.LocalDateTime;

/**
 * 결재 첨부 배치 조회({@code ApprovalQueryMapper.findFileVersionsByIds}) 결과 1행.
 *
 * <p>{@code fileDeleted} 는 버전이 아니라 <b>소유 문서</b>({@code file.deleted_at})의 상태다 —
 * 휴지통 이동·복구가 문서 단위이기 때문이다(`DELETE.md` D-6). 단건 경로
 * ({@code ApprovalFileCatalogAdapter.findFileVersion})와 판정 기준이 같아야 한다.
 */
public record ApprovalFileVersionRow(
        Long fileVersionId,
        String uploadStatus,
        String fileName,
        Long fileSize,
        LocalDateTime uploadedAt,
        boolean fileDeleted
) {
}
