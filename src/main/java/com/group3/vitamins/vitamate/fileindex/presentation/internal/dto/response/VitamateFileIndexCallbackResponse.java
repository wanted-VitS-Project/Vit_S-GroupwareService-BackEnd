package com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexCallbackResult;
import io.swagger.v3.oas.annotations.media.Schema;

// 파일 인덱싱 상태 callback 처리 결과 응답 DTO
@Schema(description = "비타메이트 파일 인덱싱 상태 callback 응답")
public record VitamateFileIndexCallbackResponse(
        @Schema(description = "상태 저장 여부", example = "true")
        boolean accepted,

        @Schema(description = "파일 버전 ID", example = "101")
        Long fileVersionId,

        @Schema(description = "처리 대상 파일 인덱싱 시도 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String indexAttemptId,

        @Schema(description = "저장된 인덱싱 상태", example = "COMPLETED")
        String indexStatus,

        @Schema(description = "accepted=false일 때 무시 사유")
        String reason
) {

    // application result를 HTTP 응답 DTO로 변환한다.
    public static VitamateFileIndexCallbackResponse from(VitamateFileIndexCallbackResult result) {
        return new VitamateFileIndexCallbackResponse(
                result.accepted(),
                result.fileVersionId(),
                result.indexAttemptId(),
                result.indexStatus(),
                result.reason()
        );
    }
}
