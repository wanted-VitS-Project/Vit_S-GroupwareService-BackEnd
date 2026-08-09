package com.group3.vitamins.employee.application.result;

/** 사번+이름 최소 참조 (일괄 등록 결과의 emailNotRegistered 목록용, EMP-019). */
public record BulkEmployeeRef(String userId, String name) {
}
