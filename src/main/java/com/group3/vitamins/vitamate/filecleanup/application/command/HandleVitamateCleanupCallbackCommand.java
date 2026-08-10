package com.group3.vitamins.vitamate.filecleanup.application.command;

// Python worker가 전달한 ChromaDB 정리 결과를 담습니다.
public record HandleVitamateCleanupCallbackCommand(
        Long cleanupJobId,
        String attemptId,
        String status,
        Boolean retryable,
        Integer deletedVectorCount,
        String errorCode,
        String errorMessage
) {
}