package com.group3.vitamins.vitamate.fileindex.application.command;

// Python worker가 전달한 파일 인덱싱 상태 callback command
public record HandleVitamateFileIndexCallbackCommand(
        Long fileVersionId,
        String indexAttemptId,
        String indexStatus,
        String errorMessage
) {
}
