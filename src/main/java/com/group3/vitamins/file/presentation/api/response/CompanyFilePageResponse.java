package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.CompanyFilePageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 전사 파일 관리(FILE-Q-01) 페이지 응답. 프로젝트 목록 페이지네이션과 동일 구조. */
@Schema(description = "전사 파일 관리 페이지 응답")
public record CompanyFilePageResponse(
        @Schema(description = "현재 페이지 항목") List<FileViewResponse> content,
        @Schema(description = "0-base 페이지") int page,
        @Schema(description = "페이지 크기") int size,
        @Schema(description = "전체 건수") long totalElements,
        @Schema(description = "전체 페이지 수") int totalPages
) {

    public static CompanyFilePageResponse from(CompanyFilePageResult r) {
        return new CompanyFilePageResponse(
                r.content().stream().map(FileViewResponse::from).toList(),
                r.page(), r.size(), r.totalElements(), r.totalPages());
    }
}
