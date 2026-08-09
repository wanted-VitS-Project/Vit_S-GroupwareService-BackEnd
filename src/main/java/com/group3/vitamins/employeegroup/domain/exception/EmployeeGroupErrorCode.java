package com.group3.vitamins.employeegroup.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * EmployeeGroup 도메인 에러 코드 (`.ai/api/employee-group.md`).
 *
 * <p>⛔ 코드·메시지는 프론트와의 계약이다. 임의로 추가·변경하지 마라 (`.ai/API.md` §0).
 * 401({@code AUTH_UNAUTHENTICATED})은 Security 필터, 403 ADMIN 판정은 {@code ACC_ADMIN_REQUIRED}(account 코드),
 * 존재하지 않는 사번은 {@code EMP_NOT_FOUND}(employee 코드)를 재사용한다 — 여기 두지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum EmployeeGroupErrorCode implements ErrorCode {

    // --- 400 ---
    GRP_INVALID_REQUEST("GRP_INVALID_REQUEST", "요청이 올바르지 않습니다."),

    // --- 404 ---
    GRP_NOT_FOUND("GRP_NOT_FOUND", "그룹을 찾을 수 없습니다."),
    GRP_MEMBER_NOT_FOUND("GRP_MEMBER_NOT_FOUND", "이 그룹의 구성원이 아닙니다."),

    // --- 409 ---
    GRP_NAME_DUPLICATED("GRP_NAME_DUPLICATED", "이미 존재하는 그룹명입니다.");

    private final String code;
    private final String message;
}
