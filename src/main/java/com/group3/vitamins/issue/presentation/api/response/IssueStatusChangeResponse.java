package com.group3.vitamins.issue.presentation.api.response;

import com.group3.vitamins.issue.application.result.IssueStatusResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "이슈 상태 변경 응답")
public record IssueStatusChangeResponse(

        @Schema(description = "이슈 ID", example = "101")
        Long issueId,

        @Schema(description = "동시 수정 검사용 변경 후 버전", example = "2")
        int version,

        @Schema(description = "TODO · IN_PROGRESS · DONE", example = "DONE")
        String status,

        @Schema(description = "완료 시각. 완료 상태가 아니면 null", example = "2026-08-02T22:46:00")
        LocalDateTime completedAt,

        @Schema(description = "최종 수정 일시", example = "2026-08-02T22:46:00")
        LocalDateTime updatedAt
) {

    public static IssueStatusChangeResponse from(IssueStatusResult result) {
        return new IssueStatusChangeResponse(
                result.issueId(),
                result.version(),
                result.status(),
                result.completedAt(),
                result.updatedAt());
    }
}
