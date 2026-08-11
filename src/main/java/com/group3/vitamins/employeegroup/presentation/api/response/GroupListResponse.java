package com.group3.vitamins.employeegroup.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 그룹 목록(§1) 응답 래퍼 — 계약이 {@code data.content[]} 이므로 배열을 {@code content} 로 감싼다. */
@Schema(description = "그룹 목록(§1)")
public record GroupListResponse(
        @Schema(description = "그룹 목록(이름 오름차순)") List<GroupItemResponse> content
) {
}
