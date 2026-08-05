package com.group3.vitamins.department.presentation.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 부서 목록 응답 (`.ai/api/department.md` §1).
 *
 * <p>⛔ <b>페이징이 없다.</b> 부서는 회사당 수십 개라 전체를 트리로 반환한다.
 * {@code content} 는 <b>최상위 부서</b> 목록이고, 하위는 각 노드의 {@code children} 에 담긴다.
 */
@Schema(name = "DepartmentList", description = "부서 목록 (최상위 부서 트리)")
public record DepartmentListResponse(
        @Schema(description = "최상위 부서 목록")
        List<DepartmentTreeResponse> content
) {
}
