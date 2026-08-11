package com.group3.vitamins.pagepermission.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 내 페이지 목록(§1) 응답 — 계약이 {@code data.content[]} 이라 배열을 content 로 감싼다. */
public record MyPagesResponse(
        @Schema(description = "노출되는 페이지 목록(카탈로그 순서)") List<MyPageResponse> content
) {
}
