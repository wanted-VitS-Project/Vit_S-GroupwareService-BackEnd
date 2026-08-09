package com.group3.vitamins.employeegroup.application.policy;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 그룹 관리(생성·수정·삭제·구성원 변경) ADMIN 권한 판정. 명세가 {@code ACC_ADMIN_REQUIRED}(account 코드)를
 * 재사용하도록 계약돼 있어 {@code @PreAuthorize} 대신 명시적으로 판정한다 (`.ai/API.md` §0, department 선례).
 *
 * <p>⚠️ 그룹 관리 주체는 명세상 미확정(ADMIN 가정, MASTER 허용 여부 팀 확인 필요). 우선 ADMIN 전용으로 둔다.
 */
@Component
@Slf4j
public class EmployeeGroupAdminPolicy {

    private static final String ROLE_ADMIN = "ADMIN";

    public void assertAdmin(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            log.warn("그룹 관리 권한 없음 - role={}", role);
            throw new ForbiddenException(AccountErrorCode.ACC_ADMIN_REQUIRED);
        }
    }
}
