package com.group3.vitamins.global.domain.common.error.exception;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;

/**
 * 비밀번호 해시 동시 실행 한도를 초과해 대기 시간 안에 자리를 못 잡은 경우.
 *
 * <p>서버가 죽는 대신 일부 요청을 되돌려보내는 것이므로 <b>503 (일시적 과부하)</b> 이 맞다.
 * 400 대가 아니다 — 요청 자체에는 문제가 없다.
 */
public class PasswordHashingBusyException extends DomainException {

    public PasswordHashingBusyException() {
        super(Code.PASSWORD_HASHING_BUSY);
    }

    public PasswordHashingBusyException(Throwable cause) {
        super(Code.PASSWORD_HASHING_BUSY, cause);
    }

    @Override
    public int getHttpStatus() {
        return 503;
    }

    /**
     * 코드 표기는 {@code .ai/api/auth.md} 의 공통 규칙을 따른다 — {@code AUTH_{의미}} (언더스코어, 번호식 아님).
     *
     * <p>2026-08-03 노션 확정. 다른 코드들과 달리 <b>요청 내용과 무관한 서버 과부하</b>이므로
     * 프론트는 로그인 실패가 아니라 "잠시 후 재시도" 로 처리한다.
     */
    enum Code implements ErrorCode {

        PASSWORD_HASHING_BUSY("AUTH_HASHING_BUSY", "요청이 많아 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");

        private final String code;
        private final String message;

        Code(String code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
