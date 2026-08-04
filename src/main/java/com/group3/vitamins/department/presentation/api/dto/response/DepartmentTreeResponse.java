package com.group3.vitamins.department.presentation.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 부서 목록 트리 노드 (`.ai/api/department.md` §1).
 *
 * <p>계층은 최대 2단이라 {@code children} 의 원소는 다시 자식을 갖지 않는다 (항상 빈 배열).
 *
 * @param departmentId        부서 번호
 * @param name                부서명
 * @param directEmployeeCount 직속 사원 수 → 삭제 차단 판정
 * @param totalEmployeeCount  하위 포함 사원 수 → 화면에 표시하는 값
 * @param children            하위 부서 (없으면 빈 배열)
 */
@Schema(name = "DepartmentTreeNode", description = "부서 트리 노드 (최대 2단)")
public record DepartmentTreeResponse(
        @Schema(description = "부서 번호", example = "1")
        Long departmentId,

        @Schema(description = "부서명", example = "경영지원본부")
        String name,

        @Schema(description = "직속 사원 수 (시스템 계정·퇴사자 제외)", example = "0")
        int directEmployeeCount,

        @Schema(description = "하위 포함 사원 수 (화면 표시값)", example = "4")
        int totalEmployeeCount,

        @Schema(description = "하위 부서. 없으면 빈 배열")
        List<DepartmentTreeResponse> children
) {
}
