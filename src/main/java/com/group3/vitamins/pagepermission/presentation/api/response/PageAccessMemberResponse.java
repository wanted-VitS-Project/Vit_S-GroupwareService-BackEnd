package com.group3.vitamins.pagepermission.presentation.api.response;

import com.group3.vitamins.pagepermission.application.result.PageAccessMemberResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이지 접근 가능자(§3) 항목. revocable=false 는 전역 권한이라 회수 불가.")
public record PageAccessMemberResponse(
        @Schema(description = "사번") String userId,
        @Schema(description = "이름") String name,
        @Schema(description = "부서 경로 '기술본부 / 개발팀' (null 허용)") String departmentPath,
        @Schema(description = "직급명 (null 허용)") String jobPositionName,
        @Schema(description = "전역 권한 role") String role,
        @Schema(description = "권한 등급 VIEWER·EDITOR") String permission,
        @Schema(description = "근거 GRANTED·GLOBAL_ROLE") String source,
        @Schema(description = "회수 가능 여부(GLOBAL_ROLE 은 false)") boolean revocable
) {
    public static PageAccessMemberResponse from(PageAccessMemberResult r) {
        return new PageAccessMemberResponse(
                r.userId(), r.name(), r.departmentPath(), r.jobPositionName(),
                r.role(), r.permission(), r.source(), r.revocable());
    }
}
