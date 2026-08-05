package com.group3.vitamins.department.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Department 도메인 에러 코드.
 *
 * <p>⛔ <b>여기 있는 코드와 메시지는 프론트와의 계약이다</b> (`.ai/api/department.md`).
 * 임의로 추가·변경하지 마라. 필요하면 md 명세를 먼저 고친다 (`.ai/API.md` §0).
 *
 * <p>공통 코드는 여기 두지 않는다 —
 * 401({@code AUTH_UNAUTHENTICATED}) 은 Security 필터에서 넘어와 {@code GlobalExceptionHandler} 가 처리하고,
 * 403 ADMIN 판정은 명세가 {@code ACC_ADMIN_REQUIRED}(account 도메인 코드)를 재사용하도록 계약돼 있다.
 */
@Getter
@RequiredArgsConstructor
public enum DepartmentErrorCode implements ErrorCode {

    // --- 400 ---
    /** 부서명이 비었거나 50자를 초과 */
    DEPT_INVALID_REQUEST("DEPT_INVALID_REQUEST", "부서명은 1자 이상 50자 이하여야 합니다."),

    // --- 404 ---
    /** 하위 부서 생성 시 상위 부서가 존재하지 않음 */
    DEPT_PARENT_NOT_FOUND("DEPT_PARENT_NOT_FOUND", "상위 부서를 찾을 수 없습니다."),
    DEPT_NOT_FOUND("DEPT_NOT_FOUND", "부서를 찾을 수 없습니다."),

    // --- 409 ---
    /** 이미 존재하는 부서명 (전체에서 유니크) */
    DEPT_NAME_DUPLICATED("DEPT_NAME_DUPLICATED", "이미 존재하는 부서명입니다."),
    /** 하위 부서를 상위로 지정 — 계층은 최대 2단 */
    DEPT_MAX_DEPTH_EXCEEDED("DEPT_MAX_DEPTH_EXCEEDED", "부서 계층은 최대 2단까지만 가능합니다."),
    /** 직속 사원이 있어 삭제 불가 — 메시지에 인원 수를 담는다 */
    DEPT_HAS_EMPLOYEES("DEPT_HAS_EMPLOYEES", "소속 사원이 있어 삭제할 수 없습니다."),
    /** 하위 부서가 있어 삭제 불가 — 메시지에 하위 부서 수를 담는다 */
    DEPT_HAS_CHILDREN("DEPT_HAS_CHILDREN", "하위 부서가 있어 삭제할 수 없습니다.");

    private final String code;
    private final String message;
}
