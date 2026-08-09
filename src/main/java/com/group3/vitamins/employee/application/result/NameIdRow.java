package com.group3.vitamins.employee.application.result;

/** 이름 → ID 조회 행 (엑셀 일괄 등록의 부서명·직급명 해석용). MyBatis 매핑 대상. */
public record NameIdRow(String name, Long id) {
}
