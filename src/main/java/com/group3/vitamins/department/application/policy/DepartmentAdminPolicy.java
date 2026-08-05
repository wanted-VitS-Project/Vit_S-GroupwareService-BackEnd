package com.group3.vitamins.department.application.policy;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 부서 관리(생성·수정·삭제) ADMIN 권한 판정.
 *
 * <p>Security 필터는 인증(세션 유무)만 보므로 ADMIN 판정은 여기서 명시적으로 한다. 명세가 일반 403 이 아니라
 * account 도메인 코드({@code ACC_ADMIN_REQUIRED})를 재사용하도록 계약돼 있다. {@code @PreAuthorize} 를 쓰면
 * 전역 핸들러가 {@code COMMON_FORBIDDEN} 으로 내려 명세 코드가 나가지 않는다 (`.ai/API.md` §0).
 */
@Component
@Slf4j
public class DepartmentAdminPolicy {

    private static final String ROLE_ADMIN = "ADMIN";

    /** 전역 role 이 ADMIN 인지 검사한다. MASTER 도 통과하지 못한다. */
    public void assertAdmin(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            log.warn("부서 관리 권한 없음 - role={}", role);
            throw new ForbiddenException(AccountErrorCode.ACC_ADMIN_REQUIRED);
        }
    }
}
