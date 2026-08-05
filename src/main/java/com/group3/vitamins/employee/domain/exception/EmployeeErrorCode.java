package com.group3.vitamins.employee.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사원 도메인 에러코드 (`.ai/api/employee.md`).
 *
 * <p>목록·상세(#121)까지 구현돼 있다. 등록·수정·퇴사(#122~)의 코드는 그 시점에 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements ErrorCode {

    EMP_INVALID_PARAMETER("EMP_INVALID_PARAMETER",
            "요청 파라미터가 올바르지 않습니다."),

    EMP_NOT_FOUND("EMP_NOT_FOUND",
            "사원을 찾을 수 없습니다.");

    private final String code;
    private final String message;
}
