package com.group3.vitamins.companydocument.domain.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

/**
 * 미리보기 생성 실패(§9 · {@code CDOC_PREVIEW_FAILED}) — 500.
 * 전역 예외 세트에 500 매핑이 없어 도메인이 정의한다(계약이 500 을 요구). file 의 {@code FilePreviewException} 과 동형.
 */
public class CompanyDocumentPreviewException extends DomainException {

    public CompanyDocumentPreviewException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    @Override
    public int getHttpStatus() {
        return 500;
    }
}
