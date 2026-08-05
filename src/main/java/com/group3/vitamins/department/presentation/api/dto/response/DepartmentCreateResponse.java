package com.group3.vitamins.department.presentation.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 부서 생성 결과 (`.ai/api/department.md` §2).
 *
 * <p>갓 만든 부서라 인원 수는 항상 0 이지만, 목록 노드와 필드를 맞춰 프론트가 응답을 그대로
 * 트리에 꽂을 수 있게 {@code directEmployeeCount}·{@code totalEmployeeCount} 를 함께 내려준다.
 * {@code parentName} 은 최상위 부서면 {@code null} 이다.
 *
 * @param departmentId        생성된 부서 번호
 * @param name                부서명
 * @param parentId            상위 부서 번호 ({@code null} 이면 최상위)
 * @param parentName          상위 부서명 ({@code null} 이면 최상위)
 * @param directEmployeeCount 직속 사원 수 — 생성 직후이므로 0
 * @param totalEmployeeCount  하위 포함 사원 수 — 생성 직후이므로 0
 */
@Schema(name = "DepartmentCreate", description = "부서 생성 결과")
public record DepartmentCreateResponse(
        @Schema(description = "생성된 부서 번호", example = "6")
        Long departmentId,

        @Schema(description = "부서명", example = "인사팀")
        String name,

        @Schema(description = "상위 부서 번호. 최상위면 null", example = "1")
        Long parentId,

        @Schema(description = "상위 부서명. 최상위면 null", example = "경영지원본부")
        String parentName,

        @Schema(description = "직속 사원 수 (생성 직후 0)", example = "0")
        int directEmployeeCount,

        @Schema(description = "하위 포함 사원 수 (생성 직후 0)", example = "0")
        int totalEmployeeCount
) {
}
