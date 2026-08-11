package com.group3.vitamins.approval.application.port;

import java.time.LocalDateTime;

/**
 * {@code file_version} 조회 결과 중 결재 도메인이 필요로 하는 최소 정보.
 *
 * @param fileDeleted 원본 문서가 휴지통에 있는지({@code file.deleted_at}). 버전 행이 아니라
 *                    <b>문서 단위</b> 삭제 상태다 — 휴지통 이동·복구가 {@code file} 단위이기 때문이다.
 *                    이름·크기는 그대로 채운다: 결재 첨부는 증빙 이력이라 값을 감추지 않고
 *                    상태만 함께 내보낸다(`DELETE.md` D-6).
 */
public record FileVersionSummary(
        Long fileVersionId,
        String uploadStatus,
        String fileName,
        Long fileSize,
        LocalDateTime uploadedAt,
        boolean fileDeleted
) {
}
