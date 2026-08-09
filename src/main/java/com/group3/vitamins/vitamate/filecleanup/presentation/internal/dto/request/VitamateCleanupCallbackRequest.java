package com.group3.vitamins.vitamate.filecleanup.presentation.internal.dto.request;

import com.group3.vitamins.vitamate.filecleanup.application.command.HandleVitamateCleanupCallbackCommand;
import io.swagger.v3.oas.annotations.media.Schema;

// Python worker가 전달하는 ChromaDB 정리 결과 요청입니다.
@Schema(description = "ChromaDB 정리 결과 callback 요청")
public record VitamateCleanupCallbackRequest(
        @Schema(
                description = "현재 ChromaDB 정리 실행 시도 ID",
                example = "91f3c9c4-27dd-48e7-af1b-732b69eac214"
        )
        String attemptId,

        @Schema(
                description = "정리 처리 상태",
                allowableValues = {"PROCESSING", "COMPLETED", "FAILED"},
                example = "COMPLETED"
        )
        String status,

        @Schema(
                description = "실패 시 재시도 가능 여부. 실패가 아니면 false",
                example = "false"
        )
        Boolean retryable,

        @Schema(
                description = "삭제한 ChromaDB vector 수. COMPLETED일 때 필수",
                example = "12"
        )
        Integer deletedVectorCount,

        @Schema(
                description = "정제된 실패 코드. FAILED일 때 필수",
                example = "CHROMA_UNAVAILABLE"
        )
        String errorCode,

        @Schema(
                description = "정제된 실패 메시지. FAILED일 때 필수",
                example = "ChromaDB에 일시적으로 연결할 수 없습니다."
        )
        String errorMessage
) {

    // HTTP 요청값을 cleanup callback 유스케이스 입력으로 변환합니다.
    public HandleVitamateCleanupCallbackCommand toCommand(Long cleanupJobId) {
        return new HandleVitamateCleanupCallbackCommand(
                cleanupJobId,
                attemptId,
                status,
                retryable,
                deletedVectorCount,
                errorCode,
                errorMessage
        );
    }
}