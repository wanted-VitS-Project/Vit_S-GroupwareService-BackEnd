package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.EmployeeListRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 사원 목록 한 행 (`employee.md` §1). 파생 필드 3종을 여기서 계산한다.
 *
 * <ul>
 *   <li>{@code emailRegistered} — 이메일이 있으면 true. false 면 초기 비밀번호를 못 받아 로그인 불가(⚠ 미등록 배지).</li>
 *   <li>{@code departmentPath} — {@code "기술본부 / 개발팀"}. 최상위 부서면 부서명 하나만(auth 선례와 동일).</li>
 *   <li>{@code passwordStatus} — 초기 비밀번호 변경이 필요하면 RESET_REQUIRED, 아니면 NORMAL.</li>
 * </ul>
 */
public record EmployeeSummaryResponse(
        @Schema(description = "사번", example = "EMP001")
        String userId,
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "이메일 (null 허용)", example = "hong@vitamins.com")
        String email,
        @Schema(description = "이메일 등록 여부. false 면 로그인 불가", example = "true")
        boolean emailRegistered,
        @Schema(description = "부서명 (null 허용)", example = "개발팀")
        String departmentName,
        @Schema(description = "부서 경로 (2단, null 허용)", example = "기술본부 / 개발팀")
        String departmentPath,
        @Schema(description = "직급명 (null 허용)", example = "선임")
        String jobPositionName,
        @Schema(description = "전역 권한", example = "MEMBER")
        String role,
        @Schema(description = "계정 상태", example = "ACTIVE")
        String accountStatus,
        @Schema(description = "비밀번호 상태", example = "NORMAL")
        String passwordStatus,
        @Schema(description = "퇴사일 yyyy-MM-dd (null = 재직중)", example = "2026-08-01")
        String resignedAt
) {

    public static EmployeeSummaryResponse from(EmployeeListRow row) {
        return new EmployeeSummaryResponse(
                row.userId(),
                row.name(),
                row.email(),
                emailRegistered(row.email()),
                row.departmentName(),
                departmentPath(row.parentDepartmentName(), row.departmentName()),
                row.jobPositionName(),
                row.role(),
                row.accountStatus(),
                passwordStatus(row.mustChangePassword()),
                formatDate(row.resignedAt()));
    }

    /** 이메일이 있고 공백이 아니면 등록된 것으로 본다. */
    public static boolean emailRegistered(String email) {
        return email != null && !email.isBlank();
    }

    /** {@code parent / self}. 상위가 없으면 부서명만, 부서 자체가 없으면 null. */
    public static String departmentPath(String parentDepartmentName, String departmentName) {
        if (departmentName == null) {
            return null;
        }
        return parentDepartmentName == null
                ? departmentName
                : parentDepartmentName + " / " + departmentName;
    }

    public static String passwordStatus(boolean mustChangePassword) {
        return mustChangePassword ? "RESET_REQUIRED" : "NORMAL";
    }

    /** LocalDate → yyyy-MM-dd 문자열 (null 은 그대로 null). */
    public static String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
