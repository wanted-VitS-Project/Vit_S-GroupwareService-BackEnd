package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.EmployeePage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 사원 목록 페이징 응답 (`employee.md` §1). {@code data.content[]} + 페이징 메타.
 */
public record EmployeePageResponse(
        @Schema(description = "사원 목록")
        List<EmployeeSummaryResponse> content,
        @Schema(description = "현재 페이지 (0-base)", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 건수", example = "42")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "3")
        int totalPages
) {

    public static EmployeePageResponse from(EmployeePage page) {
        List<EmployeeSummaryResponse> content = page.content().stream()
                .map(EmployeeSummaryResponse::from)
                .toList();

        return new EmployeePageResponse(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
