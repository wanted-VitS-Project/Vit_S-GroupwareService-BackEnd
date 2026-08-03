package com.group3.vitamins.auth.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 로그인 · 내 정보 조회가 화면에 내려줄 값 전부. 조인 한 번으로 채운다.
 *
 * <p>읽기 모델이므로 엔티티가 아니다. 애그리거트 4개(계정·사원·부서·직급)를 가로지르는데
 * JPA 로 짜면 지연 로딩으로 쿼리가 4번 나간다.
 */
public record UserProfileRow(
        String userId,
        String name,
        String role,
        boolean mustChangePassword,
        String email,
        String phone,
        String departmentName,
        /** 상위 부서명. 최상위 부서 소속이면 {@code null} */
        String parentDepartmentName,
        String jobPositionName,
        LocalDate hiredAt,
        LocalDateTime lastLoginAt
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
}
