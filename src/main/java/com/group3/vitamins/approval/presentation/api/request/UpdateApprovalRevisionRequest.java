package com.group3.vitamins.approval.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** APR-002 — 하나만 보내도 부분 수정된다 */
public record UpdateApprovalRevisionRequest(
        @Schema(description = "결재 제목 (보내지 않으면 기존 값 유지)", example = "8월 정산 결재")
        String title,

        @Schema(description = "결재 내용 (보내지 않으면 기존 값 유지)", example = "8월 정산 내역입니다.")
        String content
) {
}
