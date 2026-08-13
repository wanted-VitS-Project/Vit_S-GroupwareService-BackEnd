package com.group3.vitamins.file.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 내 프로젝트 파일 모아보기(FILE-Q-03) 응답 래퍼. 계약이 {@code data.files[]} 라 배열을 {@code files} 로 감싼다
 * (프로젝트 문서함과 통일). 프로젝트별 그룹은 프론트가 {@code projectId} 로 묶는다(정렬은 프로젝트 → 스텝 → 블록).
 */
@Schema(description = "내 프로젝트 파일 모아보기 응답")
public record MyProjectFileListResponse(
        @Schema(description = "내가 접근 가능한(스텝 VIEWER 이상) 파일 목록") List<FileViewResponse> files
) {
}
