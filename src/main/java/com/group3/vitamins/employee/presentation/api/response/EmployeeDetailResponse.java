package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.EmployeeDetailRow;
import com.group3.vitamins.employee.application.result.EmployeeGroupRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사원 상세 응답 (`employee.md` §2). 목록 필드 + 상세 전용 필드 + 소속 그룹.
 *
 * <p>파생 필드(emailRegistered · departmentPath · passwordStatus · 날짜 포맷)는
 * {@link EmployeeSummaryResponse} 의 헬퍼를 재사용해 목록과 계산 규칙을 일치시킨다.
 */
public record EmployeeDetailResponse(
        @Schema(description = "사번", example = "EMP001")
        String userId,
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "이메일 (null 허용)", example = "hong@vitamins.com")
        String email,
        @Schema(description = "이메일 등록 여부", example = "true")
        boolean emailRegistered,
        @Schema(description = "부서 ID (null 허용)", example = "3")
        Long departmentId,
        @Schema(description = "부서명 (null 허용)", example = "개발팀")
        String departmentName,
        @Schema(description = "부서 경로 (2단, null 허용)", example = "기술본부 / 개발팀")
        String departmentPath,
        @Schema(description = "직급 ID (null 허용)", example = "2")
        Long jobPositionId,
        @Schema(description = "직급명 (null 허용)", example = "선임")
        String jobPositionName,
        @Schema(description = "전역 권한", example = "MEMBER")
        String role,
        @Schema(description = "계정 상태", example = "ACTIVE")
        String accountStatus,
        @Schema(description = "비밀번호 상태", example = "NORMAL")
        String passwordStatus,
        @Schema(description = "연락처 (null 허용)", example = "010-1234-5678")
        String phone,
        @Schema(description = "입사일 yyyy-MM-dd (null 허용)", example = "2024-03-02")
        String hiredAt,
        @Schema(description = "퇴사일 yyyy-MM-dd (null = 재직중)", example = "2026-08-01")
        String resignedAt,
        @Schema(description = "마지막 로그인 (null 허용)", example = "2026-08-05T09:12:33")
        String lastLoginAt,
        @Schema(description = "소속 그룹")
        List<GroupResponse> groups
) {

    public static EmployeeDetailResponse from(EmployeeDetailRow row, List<EmployeeGroupRow> groups) {
        List<GroupResponse> groupResponses = groups.stream()
                .map(GroupResponse::from)
                .toList();

        return new EmployeeDetailResponse(
                row.userId(),
                row.name(),
                row.email(),
                EmployeeSummaryResponse.emailRegistered(row.email()),
                row.departmentId(),
                row.departmentName(),
                EmployeeSummaryResponse.departmentPath(row.parentDepartmentName(), row.departmentName()),
                row.jobPositionId(),
                row.jobPositionName(),
                row.role(),
                row.accountStatus(),
                EmployeeSummaryResponse.passwordStatus(row.mustChangePassword()),
                row.phone(),
                EmployeeSummaryResponse.formatDate(row.hiredAt()),
                EmployeeSummaryResponse.formatDate(row.resignedAt()),
                formatDateTime(row.lastLoginAt()),
                groupResponses);
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    /** 소속 그룹 한 개 (`employee.md` §2 {@code data.groups[]}). */
    public record GroupResponse(
            @Schema(description = "그룹 번호", example = "5")
            Long groupId,
            @Schema(description = "그룹명", example = "TF-신사업")
            String name
    ) {
        public static GroupResponse from(EmployeeGroupRow row) {
            return new GroupResponse(row.groupId(), row.name());
        }
    }
}
