    package com.group3.vitamins.businesscategory.application.policy;

import com.group3.vitamins.businesscategory.domain.exception.BusinessCategoryErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BusinessCategoryAdminPolicy {

    private static final String ROLE_ADMIN = "ADMIN";

    /**
     * 전역 role 이 ADMIN 인지 검사한다. MASTER 도 통과하지 못한다.
     * {@code @PreAuthorize} 를 쓰면 전역 핸들러가 COMMON_FORBIDDEN 으로 내려 명세 코드가 나가지 않는다.
     */
    public void assertAdmin(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            log.warn("카테고리 관리 권한 없음 - role={}", role);
            throw new ForbiddenException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_ADMIN_ONLY);
        }
    }
}