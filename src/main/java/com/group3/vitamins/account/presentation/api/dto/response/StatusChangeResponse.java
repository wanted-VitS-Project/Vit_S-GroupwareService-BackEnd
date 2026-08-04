package com.group3.vitamins.account.presentation.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 계정 상태 변경 결과 (`.ai/api/account.md` §2 — {@code data.userId} · {@code data.status}) */
public record StatusChangeResponse(
        @Schema(description = "대상 사번", example = "EMP001")
        String userId,
        @Schema(description = "변경된 상태", example = "INACTIVE")
        String status
) {
}
