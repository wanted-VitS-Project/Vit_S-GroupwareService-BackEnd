package com.group3.vitamins.certificate.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 자격증 마스터 에러코드 (`.ai/api/qualification.md`).
 *
 * <p>403 은 ADMIN 정책이 account {@code ACC_ADMIN_REQUIRED} 를 던진다(business_category 와 달리 account 코드 재사용).
 */
@Getter
@RequiredArgsConstructor
public enum CertificateErrorCode implements ErrorCode {

    CERT_INVALID_REQUEST("CERT_INVALID_REQUEST",
            "자격증 이름을 입력해 주세요(최대 100자)."),
    CERT_NOT_FOUND("CERT_NOT_FOUND",
            "자격증을 찾을 수 없습니다."),
    CERT_NAME_DUPLICATED("CERT_NAME_DUPLICATED",
            "같은 이름의 자격증이 이미 있습니다."),
    CERT_IN_USE("CERT_IN_USE",
            "사용 중인 자격증은 삭제할 수 없습니다.");

    private final String code;
    private final String message;
}
