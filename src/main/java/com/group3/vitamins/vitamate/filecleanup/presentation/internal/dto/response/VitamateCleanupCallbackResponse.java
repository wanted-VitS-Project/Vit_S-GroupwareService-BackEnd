package com.group3.vitamins.vitamate.filecleanup.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.filecleanup.application.result.VitamateCleanupCallbackResult;
import io.swagger.v3.oas.annotations.media.Schema;

// ChromaDB 정리 callback 처리 결과를 반환합니다.
@Schema(description = "ChromaDB 정리 결과 callback 응답")
public record VitamateCleanupCallbackResponse(
        @Schema(description = "현재 callback 반영 여부", example = "true")
        boolean accepted,

        @Schema(description = "ChromaDB 정리 작업 ID", example = "31")
        Long cleanupJobId,

        @Schema(
                description = "callback 처리 후 저장된 정리 상태",
                example = "COMPLETED"
        )
        String cleanupStatus,

        @Schema(
                description = "accepted=false일 때 callback을 무시한 사유",
                example = "attempt_mismatch_or_already_finished"
        )
        String reason
) {

    // application 결과를 내부 API 응답으로 변환합니다.
    public static VitamateCleanupCallbackResponse from(
            VitamateCleanupCallbackResult result
    ) {
        return new VitamateCleanupCallbackResponse(
                result.accepted(),
                result.cleanupJobId(),
                result.cleanupStatus(),
                result.reason()
        );
    }
}