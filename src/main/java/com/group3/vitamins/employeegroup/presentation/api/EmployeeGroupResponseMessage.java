package com.group3.vitamins.employeegroup.presentation.api;

/** 그룹 API 성공 응답 메시지 상수. */
public final class EmployeeGroupResponseMessage {

    public static final String LIST_SUCCESS = "그룹 목록 조회 성공";
    public static final String CREATED = "그룹을 생성했습니다.";
    public static final String UPDATED = "그룹을 수정했습니다.";
    public static final String DELETED = "그룹을 삭제했습니다.";
    public static final String MEMBERS_SUCCESS = "구성원 목록 조회 성공";
    public static final String MEMBERS_ADDED = "구성원을 추가했습니다.";
    public static final String MEMBER_REMOVED = "구성원을 제거했습니다.";

    private EmployeeGroupResponseMessage() {
    }
}
