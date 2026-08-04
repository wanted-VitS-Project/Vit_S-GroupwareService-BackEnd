package com.group3.vitamins.approval.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * APR-009 — 기존 전체를 이 목록으로 치환한다.
 *
 * <p>⛔ 개수·순서 검증(APPROVAL_LINE_EMPTY·APPROVAL_LINE_ORDER_INVALID)은 의도적으로 bean validation을
 * 안 쓴다 — 여기서 걸면 {@code COMMON_INVALID_REQUEST} 로 뭉개져서 명세가 정한 코드가 안 나간다.
 * 서비스 계층의 도메인 검증이 정확한 코드로 처리한다.
 */
public record UpdateApprovalLinesRequest(
        @Schema(description = "결재선 전체 목록(치환)")
        List<LineInput> lines
) {
    public record LineInput(
            @Schema(description = "결재자 구분 번호(사번)", example = "EMP2024001")
            String approverId,

            @Schema(description = "결재 순서(1부터 연속)", example = "1")
            int order
    ) {
    }
}
