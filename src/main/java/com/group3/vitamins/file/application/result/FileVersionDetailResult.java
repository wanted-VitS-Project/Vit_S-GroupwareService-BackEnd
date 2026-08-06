package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 파일 버전 상세 결과. 완료 통보(§2) 응답이자 버전 이력(§8)·단건(§11)에서 재사용할 형태다.
 * {@code name} 은 문서 표시명(file), 나머지는 버전 값.
 */
public record FileVersionDetailResult(
        Long fileId,
        Long fileVersionId,
        int versionNo,
        String name,
        String originalFileName,
        String extension,
        long sizeBytes,
        Integer pageCount,
        String comment,
        String uploaderName,
        String uploaderDepartment,
        String uploaderPosition,
        LocalDateTime completedAt
) {
}
