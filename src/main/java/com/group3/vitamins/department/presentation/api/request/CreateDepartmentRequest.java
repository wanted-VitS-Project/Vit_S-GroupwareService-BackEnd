package com.group3.vitamins.department.presentation.api.request;

import com.group3.vitamins.department.application.command.CreateDepartmentCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 부서 생성 요청 (`.ai/api/department.md` §2).
 *
 * <p>{@code + 부서 추가} 와 {@code 하위 부서 추가} 가 같은 API 다. {@code parentId} 유무로 갈린다 —
 * 생략하면 최상위 부서, 지정하면 그 부서의 하위 부서로 만든다 (계층은 최대 2단).
 *
 * <p>값 검증(비었거나 50자 초과)은 서비스에서 도메인 코드({@code DEPT_INVALID_REQUEST})로 한다.
 * Bean Validation 을 걸면 명세에 없는 코드가 나간다 (`.ai/API.md` §0).
 */
public record CreateDepartmentRequest(
        @Schema(description = "부서명. 최대 50자", example = "인사팀")
        String name,

        @Schema(description = "상위 부서 번호. 생략하면 최상위 부서", example = "1")
        Long parentId
) {

    /** 요청을 서비스 커맨드로 옮긴다. role 은 세션에서 가져온 값이라 요청 바디에 없다. */
    public CreateDepartmentCommand toCommand(String role) {
        return new CreateDepartmentCommand(role, name, parentId);
    }
}
