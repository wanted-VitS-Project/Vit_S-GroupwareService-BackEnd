package com.group3.vitamins.jobposition.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 직급 도메인 에러코드 (`.ai/api/job-position.md`).
 *
 * <p>⚠️ 403(권한)은 여기 없다 — 명세가 account 도메인 코드 {@code ACC_ADMIN_REQUIRED} 를 재사용하도록 계약돼 있다.
 * 401 {@code AUTH_UNAUTHENTICATED} 은 전역 인증 진입점이 처리한다.
 */
@Getter
@RequiredArgsConstructor
public enum JobPositionErrorCode implements ErrorCode {

    POS_INVALID_REQUEST("POS_INVALID_REQUEST",
            "직급명은 비어 있을 수 없고 30자를 넘을 수 없습니다."),

    POS_NAME_DUPLICATED("POS_NAME_DUPLICATED",
            "이미 존재하는 직급명입니다."),

    POS_NOT_FOUND("POS_NOT_FOUND",
            "직급을 찾을 수 없습니다."),

    POS_IN_USE("POS_IN_USE",
            "사용 인원이 있어 삭제할 수 없습니다.");

    private final String code;
    private final String message;
}
