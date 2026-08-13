package com.group3.vitamins.qualification.application.policy;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 학력·자격증 마스터(전공·자격증) 공용 ADMIN 권한 판정 (HR-V1 §2-G).
 *
 * <p>major·certificate 두 마스터가 공유한다. Security 필터는 인증만 보므로 ADMIN 판정을 여기서 명시한다.
 * {@code @PreAuthorize} 는 전역 핸들러가 {@code COMMON_FORBIDDEN} 으로 내려 도메인 코드가 안 나가므로 쓰지 않는다.
 * account 코드({@code ACC_ADMIN_REQUIRED})를 재사용한다(qualification.md 계약).
 */
@Component
@Slf4j
public class QualificationAdminPolicy {

    private static final String ROLE_ADMIN = "ADMIN";

    /** 전역 role 이 ADMIN 인지 검사한다. MASTER 도 통과하지 못한다. */
    public void assertAdmin(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            log.warn("학력·자격증 마스터 관리 권한 없음 - role={}", role);
            throw new ForbiddenException(AccountErrorCode.ACC_ADMIN_REQUIRED);
        }
    }
}
