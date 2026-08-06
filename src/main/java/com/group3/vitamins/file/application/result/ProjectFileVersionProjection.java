package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 프로젝트 파일 버전 목록(§11, #138) projection (MyBatis 매핑 대상).
 * AI/비타메이트 분석 선택용 read model 의 원시 행이다 — 프로젝트 전체 문서의 완료 버전을 담는다.
 * 파생값(latest·previewable)은 서비스가 계산한다. {@code indexStatus} 는 file_index LEFT JOIN 결과로,
 * 인덱스 행이 아직 없으면 {@code 'PENDING'} 으로 내려온다(COALESCE).
 */
public record ProjectFileVersionProjection(
        Long fileId,
        String name,
        Long fileVersionId,
        int versionNo,
        String originalFileName,
        String extension,
        long sizeBytes,
        Integer pageCount,
        LocalDateTime completedAt,
        String indexStatus
) {
}
