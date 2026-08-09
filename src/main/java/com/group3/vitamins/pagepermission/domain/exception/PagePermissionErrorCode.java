package com.group3.vitamins.pagepermission.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 페이지 권한 도메인 에러코드 (`.ai/api/page-permission.md`).
 *
 * <p>⚠️ 403(ADMIN 권한)은 여기 없다 — 명세가 account 코드 {@code ACC_ADMIN_REQUIRED} 를 재사용하도록 계약돼 있다.
 * 시스템 계정 거부({@code ACC_SYSTEM_ACCOUNT_NOT_ALLOWED})·없는 사번({@code EMP_NOT_FOUND})도 각 도메인 코드를 재사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum PagePermissionErrorCode implements ErrorCode {

    PAGE_NOT_FOUND("PAGE_NOT_FOUND",
            "존재하지 않는 페이지입니다."),

    PAGE_INVALID_REQUEST("PAGE_INVALID_REQUEST",
            "부여 대상이 비어 있거나 사번이 중복되었습니다."),

    PAGE_INVALID_PERMISSION("PAGE_INVALID_PERMISSION",
            "허용되지 않는 권한 등급입니다."),

    PAGE_PERMISSION_NOT_FOUND("PAGE_PERMISSION_NOT_FOUND",
            "부여된 권한이 없습니다.");

    private final String code;
    private final String message;
}
