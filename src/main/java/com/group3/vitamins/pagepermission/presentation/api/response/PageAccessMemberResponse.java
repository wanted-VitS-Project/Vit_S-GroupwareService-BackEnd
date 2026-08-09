package com.group3.vitamins.pagepermission.presentation.api.response;

import com.group3.vitamins.pagepermission.application.result.PageAccessMemberResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이지 접근 가능자(§3) 항목. revocable=false 는 전역 권한이라 회수 불가.")
public record PageAccessMemberResponse(
        @Schema(description = "사번", example = "vitas-EMP001") String userId,
        @Schema(description = "이름", example = "김철수") String name,
        @Schema(description = "부서 경로 '상위부서 / 부서'", nullable = true, example = "기술본부 / 개발팀") String departmentPath,
        @Schema(description = "직급명", nullable = true, example = "대리") String jobPositionName,
        @Schema(description = "전역 권한 role", example = "MEMBER") String role,
        @Schema(description = "권한 등급 VIEWER·EDITOR", example = "EDITOR") String permission,
        @Schema(description = "근거 GRANTED·GLOBAL_ROLE", example = "GRANTED") String source,
        @Schema(description = "회수 가능 여부(GLOBAL_ROLE 은 false)", example = "true") boolean revocable
) {
    public static PageAccessMemberResponse from(PageAccessMemberResult r) {
        return new PageAccessMemberResponse(
                r.userId(), r.name(), r.departmentPath(), r.jobPositionName(),
                r.role(), r.permission(), r.source(), r.revocable());
    }
}
