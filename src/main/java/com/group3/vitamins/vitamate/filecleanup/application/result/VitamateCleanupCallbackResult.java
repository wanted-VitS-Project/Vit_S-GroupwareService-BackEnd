package com.group3.vitamins.vitamate.filecleanup.application.result;

// ChromaDB 정리 callback의 처리 결과를 반환합니다.
public record VitamateCleanupCallbackResult(
        boolean accepted,
        Long cleanupJobId,
        String cleanupStatus,
        String reason
) {

    // 현재 시도의 callback이 정상 반영된 결과를 생성합니다.
    public static VitamateCleanupCallbackResult accepted(
            Long cleanupJobId,
            String cleanupStatus
    ) {
        return new VitamateCleanupCallbackResult(
                true,
                cleanupJobId,
                cleanupStatus,
                null
        );
    }

    // 오래된 시도 등의 이유로 callback이 무시된 결과를 생성합니다.
    public static VitamateCleanupCallbackResult rejected(
            Long cleanupJobId,
            String cleanupStatus,
            String reason
    ) {
        return new VitamateCleanupCallbackResult(
                false,
                cleanupJobId,
                cleanupStatus,
                reason
        );
    }
}