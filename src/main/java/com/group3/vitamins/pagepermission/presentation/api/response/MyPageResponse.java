package com.group3.vitamins.pagepermission.presentation.api.response;

import com.group3.vitamins.pagepermission.application.result.MyPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 페이지 목록(§1) 항목. permission=NONE 이면 노출되나 접근 불가.")
public record MyPageResponse(
        @Schema(description = "페이지 코드") String pageCode,
        @Schema(description = "페이지 표시명") String name,
        @Schema(description = "권한 등급 NONE·VIEWER·EDITOR") String permission,
        @Schema(description = "근거 GRANTED·GLOBAL_ROLE·ADMIN_ONLY·DEFAULT") String source
) {
    public static MyPageResponse from(MyPageResult r) {
        return new MyPageResponse(r.pageCode(), r.name(), r.permission(), r.source());
    }
}
