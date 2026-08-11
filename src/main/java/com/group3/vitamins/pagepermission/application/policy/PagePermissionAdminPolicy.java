package com.group3.vitamins.pagepermission.application.policy;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * 페이지 권한 관리 API(§2~§5)의 ADMIN 판정. 명세가 일반 403 이 아니라 도메인 코드 {@code ACC_ADMIN_REQUIRED} 를
 * 요구하므로 {@code @PreAuthorize} 대신 이 정책으로 막는다(account·직급·부서 도메인과 통일).
 */
@Component
public class PagePermissionAdminPolicy {

    private static final String ADMIN = "ADMIN";

    public void assertAdmin(String role) {
        if (!ADMIN.equals(role)) {
            throw new ForbiddenException(AccountErrorCode.ACC_ADMIN_REQUIRED);
        }
    }
}
