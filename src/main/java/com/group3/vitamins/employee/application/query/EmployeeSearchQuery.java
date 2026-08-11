package com.group3.vitamins.employee.application.query;

/**
 * 사원 이름 검색 쿼리 (`.ai/api/employee.md` §9). 결재선 지정 화면의 결재자 자동완성용.
 *
 * <p>공백·빈 문자열 검색어를 null 로 눕혀 "검색어 없음" 판정을 한 곳으로 모은다. 서비스가 null 이면 400 을 낸다.
 */
public record EmployeeSearchQuery(
        String name
) {

    public EmployeeSearchQuery {
        name = (name == null || name.isBlank()) ? null : name.trim();
    }
}
