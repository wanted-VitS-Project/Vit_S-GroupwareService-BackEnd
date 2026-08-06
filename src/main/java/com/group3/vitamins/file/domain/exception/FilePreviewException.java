package com.group3.vitamins.file.domain.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

/**
 * 미리보기 생성 실패(§10 · {@code FILE_PREVIEW_FAILED}) — 500.
 * 전역 예외 세트에 500 매핑이 없어 파일 도메인이 정의한다(계약이 500 을 요구).
 */
public class FilePreviewException extends DomainException {

    public FilePreviewException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    @Override
    public int getHttpStatus() {
        return 500;
    }
}
