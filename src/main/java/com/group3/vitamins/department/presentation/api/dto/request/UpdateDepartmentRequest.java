package com.group3.vitamins.department.presentation.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 부서명 수정 요청 (`.ai/api/department.md` §3).
 *
 * <p>⛔ 상위 부서는 바꿀 수 없다 — 부서 이동 기능이 없다. 그래서 {@code name} 만 받는다.
 *
 * <p>값 검증(비었거나 50자 초과)은 서비스에서 도메인 코드({@code DEPT_INVALID_REQUEST})로 한다.
 */
public record UpdateDepartmentRequest(
        @Schema(description = "새 부서명. 최대 50자", example = "인사기획팀")
        String name
) {
}
