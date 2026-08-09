package com.group3.vitamins.employeegroup.application.result;

/** §6 구성원 추가 검증용 — 요청 사번의 존재·시스템 계정 여부 (MyBatis). */
public record EmployeeRefRow(String userId, boolean isSystem) {
}
