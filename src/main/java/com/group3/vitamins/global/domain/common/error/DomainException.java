package com.group3.vitamins.global.domain.common.error;

public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 코드는 그대로 두고 메시지만 상황에 맞게 덮어쓴다.
     *
     * <p>예: {@code AUTH_ACCOUNT_LOCKED} 는 명세상 <b>메시지에 잠금 해제 시각을 담아야</b> 한다
     * (`.ai/api/auth.md`). 코드가 바뀌면 프론트 분기가 깨지므로 <b>메시지만</b> 바꾼다.
     */
    protected DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public abstract int getHttpStatus();
}
