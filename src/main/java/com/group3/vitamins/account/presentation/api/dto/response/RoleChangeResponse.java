package com.group3.vitamins.account.presentation.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 전역 권한 변경 결과 (`.ai/api/account.md` §1 — {@code data.userId} · {@code data.role}) */
public record RoleChangeResponse(
        @Schema(description = "대상 사번", example = "EMP001")
        String userId,
        @Schema(description = "변경된 권한", example = "MASTER")
        String role
) {
}
