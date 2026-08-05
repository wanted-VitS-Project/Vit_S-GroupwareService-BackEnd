package com.group3.vitamins.department.presentation.api;

/**
 * 부서 API 성공 응답 메시지 상수 (`.ai/api/department.md`).
 */
public final class DepartmentResponseMessage {

    private DepartmentResponseMessage() {
    }

    public static final String LIST_SUCCESS = "부서 목록 조회 성공";
    public static final String CREATED = "부서가 생성되었습니다.";
    public static final String UPDATED = "부서명이 수정되었습니다.";
    public static final String DELETED = "부서가 삭제되었습니다.";
}
