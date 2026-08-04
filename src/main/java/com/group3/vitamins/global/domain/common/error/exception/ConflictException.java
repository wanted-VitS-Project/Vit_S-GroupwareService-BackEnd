package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

public class ConflictException extends DomainException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 코드는 그대로 두고 메시지만 상황에 맞게 덮어쓴다.
     *
     * <p>예: {@code DEPT_HAS_EMPLOYEES} 는 명세상 <b>메시지에 인원 수를 담아야</b> 한다
     * (`.ai/api/department.md` §4). 코드가 바뀌면 프론트 분기가 깨지므로 <b>메시지만</b> 바꾼다.
     */
    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ConflictException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
