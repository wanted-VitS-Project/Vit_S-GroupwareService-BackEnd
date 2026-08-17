package com.group3.vitamins.employee.application.query;

/**
 * 사원 후보 검색 쿼리 (`.ai/api/employee.md` §9). 결재선·참여자 지정 화면의 후보 조회용.
 *
 * <p>이름 부분 일치 <b>또는</b> 부서로 후보를 좁힌다 — 이름을 모를 때도 부서로 후보를 펼칠 수 있게
 * {@code name} 을 선택값으로 열었다(2026-08-17, A안). 서비스가 <b>둘 다 없으면</b> 400 을 낸다.
 *
 * <p>공백·빈 문자열 검색어는 null 로 눕혀 "이름 없음" 판정을 한 곳으로 모은다.
 */
public record EmployeeSearchQuery(
        String name,
        Long departmentId
) {

    public EmployeeSearchQuery {
        name = (name == null || name.isBlank()) ? null : name.trim();
    }
}
