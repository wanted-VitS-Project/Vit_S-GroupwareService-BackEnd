package com.group3.vitamins.auth.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 로그인 · 내 정보 조회가 화면에 내려줄 값 전부. 조인 한 번으로 채운다.
 *
 * <p>읽기 모델이므로 엔티티가 아니다. 애그리거트 4개(계정·사원·부서·직급)를 가로지르는데
 * JPA 로 짜면 지연 로딩으로 쿼리가 4번 나간다.
 * ({@link com.group3.vitamins.auth.application.port.UserProfileQueryPort} 의 반환 타입)
 */
public record UserProfileRow(
        String userId,
        String name,
        String role,
        boolean mustChangePassword,
        /** 약관 동의 시각. {@code null} 이면 미동의 (`auth.md` §5) */
        LocalDateTime termsAgreedAt,
        String email,
        String phone,
        String departmentName,
        /** 상위 부서명. 최상위 부서 소속이면 {@code null} */
        String parentDepartmentName,
        String jobPositionName,
        LocalDate hiredAt,
        LocalDateTime lastLoginAt,
        /** 소속 회사(테넌트) 번호 — 로그인 시 세션(TenantContext)에 실린다. 화면에 노출하지 않는다 */
        Long companyId
) {

    /** 명세의 {@code departmentPath} — {@code "기술본부 / 개발팀"}. 최상위면 부서명 하나만 */
    public String departmentPath() {
        if (departmentName == null) {
            return null;
        }
        return parentDepartmentName == null
                ? departmentName
                : parentDepartmentName + " / " + departmentName;
    }

    /** 명세의 {@code passwordStatus} */
    public String passwordStatus() {
        return mustChangePassword ? "RESET_REQUIRED" : "NORMAL";
    }

    /**
     * 약관 동의가 필요한가 — 최초 로그인이고 ADMIN 이 아닐 때만 (`auth.md` §5 · §6-7).
     * ADMIN 은 공용 계정이라 약관 대상이 아니다.
     */
    public boolean termsAgreementRequired() {
        return termsAgreedAt == null && !"ADMIN".equals(role);
    }

    /** 명세의 {@code termsStatus} — ADMIN·동의 완료는 {@code AGREED}, 그 외 미동의는 {@code REQUIRED} */
    public String termsStatus() {
        return termsAgreementRequired() ? "REQUIRED" : "AGREED";
    }
}
