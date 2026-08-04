package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UpdateApprovalRevisionResponse(
        @Schema(description = "회차 구분 번호", example = "1")
        Long revisionId,

        @Schema(description = "결재 제목", example = "8월 정산 결재")
        String title,

        @Schema(description = "결재 내용", example = "8월 정산 내역입니다.")
        String content,

        @Schema(description = "수정 일시", example = "2026-08-04T13:00:00")
        LocalDateTime updatedAt
) {

    public static UpdateApprovalRevisionResponse from(ApprovalRevision revision) {
        return new UpdateApprovalRevisionResponse(
                revision.getRevisionId(), revision.getTitle(), revision.getContent(), revision.getUpdatedAt());
    }
}
