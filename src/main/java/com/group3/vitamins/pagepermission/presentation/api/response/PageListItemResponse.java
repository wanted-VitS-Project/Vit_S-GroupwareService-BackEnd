package com.group3.vitamins.pagepermission.presentation.api.response;

import com.group3.vitamins.pagepermission.application.result.PageListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "페이지 목록(§2) 항목 — 부여 가능한 페이지와 접근 인원 집계.")
public record PageListItemResponse(
        @Schema(description = "페이지 코드") String pageCode,
        @Schema(description = "페이지명") String name,
        @Schema(description = "설명") String description,
        @Schema(description = "접근 인원 = grantedCount + globalRoleCount") int accessCount,
        @Schema(description = "명시적 부여 인원") int grantedCount,
        @Schema(description = "전역 권한 열람 인원") int globalRoleCount,
        @Schema(description = "마지막 수정일 yyyy-MM-dd (null 허용)") String lastModifiedAt
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static PageListItemResponse from(PageListItemResult r) {
        return new PageListItemResponse(
                r.pageCode(), r.name(), r.description(), r.accessCount(), r.grantedCount(), r.globalRoleCount(),
                r.lastModifiedAt() == null ? null : r.lastModifiedAt().format(FMT));
    }
}
