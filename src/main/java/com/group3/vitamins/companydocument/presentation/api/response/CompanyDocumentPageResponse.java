package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 사내 문서 목록(§3) 페이지 응답. FILE-Q 전사 파일과 동일 구조. */
@Schema(description = "사내 문서 목록 페이지 응답")
public record CompanyDocumentPageResponse(
        @Schema(description = "현재 페이지 항목") List<CompanyDocumentListItemResponse> content,
        @Schema(description = "0-base 페이지") int page,
        @Schema(description = "페이지 크기") int size,
        @Schema(description = "전체 건수") long totalElements,
        @Schema(description = "전체 페이지 수") int totalPages
) {

    public static CompanyDocumentPageResponse from(CompanyDocumentPageResult r) {
        return new CompanyDocumentPageResponse(
                r.content().stream().map(CompanyDocumentListItemResponse::from).toList(),
                r.page(), r.size(), r.totalElements(), r.totalPages());
    }
}
