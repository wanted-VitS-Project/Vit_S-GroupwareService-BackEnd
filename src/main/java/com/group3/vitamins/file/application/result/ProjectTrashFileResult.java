package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 프로젝트 휴지통 모아보기(§13) 결과 항목. 휴지통 문서 1건 + 위치(스텝·블록) + 휴지통 진입 시각.
 * 고아 파일이면 blockId·blockTitle 이 null 이고 blockDeleted=true. 다운로드 진입점(previewable·latestVersionId 등)은
 * 두지 않는다 — 휴지통에서는 복구(§6)·영구삭제(§7)만 한다.
 */
public record ProjectTrashFileResult(
        Long stepId,
        String stepName,
        Long blockId,
        String blockTitle,
        boolean blockDeleted,
        Long fileId,
        String name,
        int versionCount,
        String originalFileName,
        String extension,
        long sizeBytes,
        LocalDateTime deletedAt
) {
}
