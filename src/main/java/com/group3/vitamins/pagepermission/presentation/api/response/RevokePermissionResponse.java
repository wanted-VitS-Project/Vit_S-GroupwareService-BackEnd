package com.group3.vitamins.pagepermission.presentation.api.response;

import com.group3.vitamins.pagepermission.application.result.RevokeResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이지 권한 회수(§5) 결과. ADMIN·MASTER 는 회수 후에도 전역 권한으로 열람 가능.")
public record RevokePermissionResponse(
        @Schema(description = "페이지 코드") String pageCode,
        @Schema(description = "대상 사번") String userId,
        @Schema(description = "회수 후에도 접근 가능한지(MASTER 면 true)") boolean stillAccessible,
        @Schema(description = "접근 가능 시 근거 GLOBAL_ROLE, 아니면 null") String accessSource
) {
    public static RevokePermissionResponse from(RevokeResult r) {
        return new RevokePermissionResponse(r.pageCode(), r.userId(), r.stillAccessible(), r.accessSource());
    }
}
