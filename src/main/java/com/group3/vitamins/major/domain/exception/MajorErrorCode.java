package com.group3.vitamins.major.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 전공 마스터 에러코드 (`.ai/api/qualification.md`).
 *
 * <p>403 은 ADMIN 정책이 account {@code ACC_ADMIN_REQUIRED} 를 던진다(business_category 와 달리 account 코드 재사용).
 */
@Getter
@RequiredArgsConstructor
public enum MajorErrorCode implements ErrorCode {

    MAJOR_INVALID_REQUEST("MAJOR_INVALID_REQUEST",
            "전공 이름을 입력해 주세요(최대 100자)."),
    MAJOR_NOT_FOUND("MAJOR_NOT_FOUND",
            "전공을 찾을 수 없습니다."),
    MAJOR_NAME_DUPLICATED("MAJOR_NAME_DUPLICATED",
            "같은 이름의 전공이 이미 있습니다."),
    MAJOR_IN_USE("MAJOR_IN_USE",
            "사용 중인 전공은 삭제할 수 없습니다.");

    private final String code;
    private final String message;
}
