package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.request;

import com.group3.vitamins.vitamate.fileindex.application.command.HandleVitamateFileIndexCallbackCommand;
import io.swagger.v3.oas.annotations.media.Schema;

// Python worker가 파일 인덱싱 상태를 전달하는 callback 요청 DTO
@Schema(description = "비타메이트 파일 인덱싱 상태 callback 요청")
public record VitamateFileIndexCallbackRequest(
        @Schema(description = "Spring이 발급한 현재 파일 인덱싱 시도 ID. COMPLETED/FAILED callback에서는 필수", example = "550e8400-e29b-41d4-a716-446655440000")
        String indexAttemptId,

        @Schema(description = "파일 인덱싱 상태", allowableValues = {"PROCESSING", "COMPLETED", "FAILED"}, example = "COMPLETED")
        String indexStatus,

        @Schema(description = "인덱싱 실패 사유. FAILED일 때 필수", example = "PDF 텍스트 추출에 실패했습니다.")
        String errorMessage
) {

    // HTTP 요청 값을 application command로 변환한다.
    public HandleVitamateFileIndexCallbackCommand toCommand(Long fileVersionId) {
        return new HandleVitamateFileIndexCallbackCommand(
                fileVersionId,
                indexAttemptId,
                indexStatus,
                errorMessage
        );
    }
}
