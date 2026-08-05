package com.group3.vitamins.employee.presentation.api;

/**
 * 사원 API 성공 응답 메시지 상수 (`.ai/api/employee.md`).
 */
public final class EmployeeResponseMessage {

    private EmployeeResponseMessage() {
    }

    public static final String SEARCH_SUCCESS = "사원 검색 성공";
    public static final String LIST_SUCCESS = "사원 목록 조회 성공";
    public static final String DETAIL_SUCCESS = "사원 상세 조회 성공";
    public static final String REGISTERED = "사원 등록 성공";
    public static final String UPDATED = "사원 정보 수정 성공";
    public static final String RESIGNED = "퇴사 처리 성공";
}
