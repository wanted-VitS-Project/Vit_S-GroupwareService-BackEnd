package com.group3.vitamins.file.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 프로젝트 전체 파일 모아보기(§12) 응답 래퍼. 계약이 {@code data.files[]} 이므로 배열을 {@code files} 로 감싼다
 * (이미지 {@code 프로젝트 이미지 모아보기}의 {@code data.images[]} 와 구조 통일). ⚠️ 배열을 {@code data} 로 바로 내리면
 * 프론트가 {@code data.files} 를 못 읽는다 — §11(버전목록)은 {@code data[]} 평면이라 감싸지 않지만 §12 는 감싼다.
 */
public record ProjectFileListResponse(
        @Schema(description = "프로젝트 내 전체 파일 목록(문서 단위 최신 버전)")
        List<ProjectFileResponse> files
) {
}
