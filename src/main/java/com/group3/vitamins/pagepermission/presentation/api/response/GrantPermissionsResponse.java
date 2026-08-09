package com.group3.vitamins.pagepermission.presentation.api.response;

import com.group3.vitamins.pagepermission.application.result.GrantResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이지 권한 부여(§4) 결과. requested = granted(신규) + updated(등급변경) + unchanged(무변화).")
public record GrantPermissionsResponse(
        @Schema(description = "페이지 코드") String pageCode,
        @Schema(description = "요청 인원") int requestedCount,
        @Schema(description = "신규 부여 인원") int grantedCount,
        @Schema(description = "등급 변경 인원") int updatedCount,
        @Schema(description = "변화 없음 인원") int unchangedCount
) {
    public static GrantPermissionsResponse from(GrantResult r) {
        return new GrantPermissionsResponse(
                r.pageCode(), r.requestedCount(), r.grantedCount(), r.updatedCount(), r.unchangedCount());
    }
}
