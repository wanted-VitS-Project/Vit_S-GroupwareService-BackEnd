package com.group3.vitamins.employee.application.result;

import java.time.LocalDate;

/**
 * 사원 목록 한 행 (`employee.md` §1). 계정·사원·부서(자기+상위)·직급을 가로지르는 MyBatis 조회 결과.
 *
 * <p>순수 projection 이다 — 파생 필드({@code emailRegistered} · {@code departmentPath} ·
 * {@code passwordStatus})는 응답({@code EmployeeSummaryResponse})에서 계산한다.
 *
 * @param userId               사번
 * @param name                 이름
 * @param email                이메일 (null 허용)
 * @param departmentName       부서명 (null 허용)
 * @param parentDepartmentName 상위 부서명 (null 허용, 경로 조립용)
 * @param jobPositionName      직급명 (null 허용)
 * @param role                 전역 권한 (MASTER · MEMBER)
 * @param accountStatus        계정 상태 (ACTIVE · INACTIVE)
 * @param mustChangePassword   초기 비밀번호 변경 필요 여부
 * @param resignedAt           퇴사일 (null = 재직중)
 * @param profileImageKey      프로필 사진 S3 키 (null = 사진 없음). 아바타 URL 노출 판정용 — 키 자체는 응답에 안 나간다
 */
public record EmployeeListRow(
        String userId,
        String name,
        String email,
        String departmentName,
        String parentDepartmentName,
        String jobPositionName,
        String role,
        String accountStatus,
        boolean mustChangePassword,
        LocalDate resignedAt,
        String profileImageKey
) {
}
