package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalHistoryResult;
import com.group3.vitamins.approval.application.result.ApprovalRevisionHistoryItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "결재 이력 응답")
public record ApprovalHistoryResponse(

        @Schema(description = "회차 목록(회차 번호 오름차순)")
        List<Item> content
) {

    public record Item(
            @Schema(description = "회차 구분 번호", example = "55")
            Long revisionId,

            @Schema(description = "회차 번호", example = "1")
            int revisionNo,

            @Schema(description = "회차 상태", example = "REJECTED")
            String status,

            @Schema(description = "상신 일시", example = "2026-07-20T09:00:00")
            LocalDateTime submittedAt,

            @Schema(description = "종료 일시", example = "2026-07-21T11:00:00")
            LocalDateTime finishedAt,

            @Schema(description = "현재 진행 중인 회차 여부", example = "false")
            boolean isCurrent
    ) {

        public static Item from(ApprovalRevisionHistoryItem r) {
            return new Item(r.revisionId(), r.revisionNo(), r.status(), r.submittedAt(), r.finishedAt(), r.isCurrent());
        }
    }

    public static ApprovalHistoryResponse from(ApprovalHistoryResult result) {
        return new ApprovalHistoryResponse(result.content().stream().map(Item::from).toList());
    }
}
