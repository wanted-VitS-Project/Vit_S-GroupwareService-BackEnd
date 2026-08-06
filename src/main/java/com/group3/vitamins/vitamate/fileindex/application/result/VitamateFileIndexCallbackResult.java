package com.group3.vitamins.vitamate.fileindex.application.result;

// 파일 인덱싱 상태 callback 처리 결과
public record VitamateFileIndexCallbackResult(
        boolean accepted,
        Long fileVersionId,
        String indexStatus,
        String reason
) {
}