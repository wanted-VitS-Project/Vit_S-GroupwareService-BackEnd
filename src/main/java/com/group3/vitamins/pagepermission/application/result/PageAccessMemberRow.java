package com.group3.vitamins.pagepermission.application.result;

/**
 * §3 접근 가능자 명단의 MyBatis projection. {@code permission} 은 명시 부여자(GRANTED)만 값이 있고
 * 전역 권한(MASTER)은 {@code null} → 서비스가 EDITOR 로 채운다. departmentPath 는 두 이름을 서비스가 조립한다.
 */
public record PageAccessMemberRow(
        String userId,
        String name,
        String departmentName,
        String parentDepartmentName,
        String jobPositionName,
        String role,
        String permission
) {
}
