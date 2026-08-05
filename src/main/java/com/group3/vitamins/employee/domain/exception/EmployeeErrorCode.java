package com.group3.vitamins.employee.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사원 도메인 에러코드 (`.ai/api/employee.md`).
 *
 * <p>지금은 사원 이름 검색(#137, §9)만 구현돼 있어 그 경로가 쓰는 코드만 둔다.
 * 목록·상세·등록 등 나머지 엔드포인트(#121~)는 구현 시 코드를 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements ErrorCode {

    EMP_INVALID_PARAMETER("EMP_INVALID_PARAMETER",
            "검색어(name)를 입력해 주세요.");

    private final String code;
    private final String message;
}
