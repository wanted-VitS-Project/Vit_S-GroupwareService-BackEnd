package com.group3.vitamins.employee.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사원 도메인 에러코드 (`.ai/api/employee.md`).
 *
 * <p>목록·상세(#121) + 등록(#122 PR-A)까지 구현돼 있다. 수정·퇴사(PR-B)의 코드는 그 시점에 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements ErrorCode {

    EMP_INVALID_PARAMETER("EMP_INVALID_PARAMETER",
            "요청 파라미터가 올바르지 않습니다."),

    EMP_NOT_FOUND("EMP_NOT_FOUND",
            "사원을 찾을 수 없습니다."),

    // ── 등록 (employee.md §3) ──
    EMP_INVALID_REQUEST("EMP_INVALID_REQUEST",
            "필수값이 누락되었거나 형식이 올바르지 않습니다."),

    EMP_ADMIN_ROLE_NOT_ALLOWED("EMP_ADMIN_ROLE_NOT_ALLOWED",
            "ADMIN 권한은 부여할 수 없습니다."),

    EMP_DEPARTMENT_NOT_FOUND("EMP_DEPARTMENT_NOT_FOUND",
            "부서를 찾을 수 없습니다."),

    EMP_JOB_POSITION_NOT_FOUND("EMP_JOB_POSITION_NOT_FOUND",
            "직급을 찾을 수 없습니다."),

    EMP_USER_ID_DUPLICATED("EMP_USER_ID_DUPLICATED",
            "이미 등록된 사번입니다.");

    private final String code;
    private final String message;
}
