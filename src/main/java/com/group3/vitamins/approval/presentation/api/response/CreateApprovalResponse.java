package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.domain.model.ApprovalWithRevision;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateApprovalResponse(
        @Schema(description = "요청받은 블록 구분 번호(그대로 반환)", example = "1")
        Long blockId,

        @Schema(description = "생성된 결재 구분 번호(blockId와 다른 값)", example = "1")
        Long approvalId,

        @Schema(description = "생성된 1회차 상신 구분 번호", example = "1")
        Long revisionId,

        @Schema(description = "항상 1", example = "1")
        int revisionNo,

        @Schema(description = "DRAFT", example = "DRAFT")
        String status
) {

    public static CreateApprovalResponse from(ApprovalWithRevision result) {
        return new CreateApprovalResponse(
                result.approval().getBlockId(),
                result.approval().getApprovalId(),
                result.revision().getRevisionId(),
                result.revision().getRevisionNo(),
                result.approval().getStatus().name());
    }
}
