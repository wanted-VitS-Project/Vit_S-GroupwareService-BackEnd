package com.group3.vitamins.file.application.policy;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 전사 파일 관리(FILE-Q-01) ADMIN 권한 판정.
 *
 * <p>Security 필터는 인증만 보므로 ADMIN 판정은 여기서 명시적으로 한다. {@code @PreAuthorize} 는 전역 핸들러가
 * {@code COMMON_FORBIDDEN} 으로 내려 도메인 코드가 안 나가므로 쓰지 않는다(employee·department 선례).
 * 다른 도메인과 동일하게 account 코드({@code ACC_ADMIN_REQUIRED})를 재사용한다.
 */
@Component
@Slf4j
public class FileAdminPolicy {

    private static final String ROLE_ADMIN = "ADMIN";

    /** 전역 role 이 ADMIN 인지 검사한다. MASTER 도 통과하지 못한다. */
    public void assertAdmin(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            log.warn("전사 파일 관리 권한 없음 - role={}", role);
            throw new ForbiddenException(AccountErrorCode.ACC_ADMIN_REQUIRED);
        }
    }
}
