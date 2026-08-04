package com.group3.vitamins.department.presentation.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 부서명 수정 결과 (`.ai/api/department.md` §3).
 *
 * <p>상위 부서는 바뀌지 않지만, 화면이 부서를 상위와 함께 표시하므로 {@code parentId}·{@code parentName}
 * 을 그대로 돌려준다. 최상위 부서면 둘 다 {@code null} 이다.
 *
 * @param departmentId 부서 번호
 * @param name         수정된 부서명
 * @param parentId     상위 부서 번호 ({@code null} 이면 최상위)
 * @param parentName   상위 부서명 ({@code null} 이면 최상위)
 */
@Schema(name = "DepartmentUpdate", description = "부서명 수정 결과")
public record DepartmentUpdateResponse(
        @Schema(description = "부서 번호", example = "4")
        Long departmentId,

        @Schema(description = "수정된 부서명", example = "인사기획팀")
        String name,

        @Schema(description = "상위 부서 번호. 최상위면 null", example = "1")
        Long parentId,

        @Schema(description = "상위 부서명. 최상위면 null", example = "경영지원본부")
        String parentName
) {
}
