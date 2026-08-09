package com.group3.vitamins.pagepermission.presentation.api.response;

import com.group3.vitamins.pagepermission.application.result.PageAccessListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 페이지 접근 가능자 목록(§3) 응답 — 페이지 정보 + 명단(GRANTED 먼저·이름순) + 집계. */
@Schema(description = "페이지 접근 가능자 목록(§3)")
public record PageAccessListResponse(
        @Schema(description = "페이지 코드") String pageCode,
        @Schema(description = "페이지명") String name,
        @Schema(description = "접근 가능자 명단") List<PageAccessMemberResponse> content,
        @Schema(description = "명시적 부여 인원") int grantedCount,
        @Schema(description = "전역 권한 인원") int globalRoleCount
) {
    public static PageAccessListResponse from(PageAccessListResult r) {
        return new PageAccessListResponse(
                r.pageCode(), r.name(),
                r.content().stream().map(PageAccessMemberResponse::from).toList(),
                r.grantedCount(), r.globalRoleCount());
    }
}
