package com.group3.vitamins.file.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 프로젝트 휴지통 모아보기(§13) 응답 래퍼. 계약이 {@code data.files[]} 이므로 배열을 {@code files} 로 감싼다
 * (§12 전체 모아보기 · 이미지 휴지통과 구조 통일). ⚠️ 배열을 {@code data} 로 바로 내리면 프론트가 {@code data.files} 를 못 읽는다.
 */
public record ProjectTrashFileListResponse(
        @Schema(description = "프로젝트 휴지통의 삭제 문서 목록(진입 시각 내림차순)")
        List<ProjectTrashFileResponse> files
) {
}
