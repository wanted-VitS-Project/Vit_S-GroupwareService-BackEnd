package com.group3.vitamins.pagepermission.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 페이지 목록(§2) 응답 — {@code data.content[]}. 부여 가능한 페이지(BIDDING·FINANCE)만 담긴다. */
public record PageListResponse(
        @Schema(description = "부여 가능한 페이지 목록") List<PageListItemResponse> content
) {
}
