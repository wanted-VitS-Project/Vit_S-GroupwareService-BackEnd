package com.group3.vitamins.issue.presentation.api;

public class IssueResponseMessage {

    private IssueResponseMessage() {
    }

    public static final String CREATE_SUCCESS = "이슈 생성 성공";
    public static final String DETAIL_SUCCESS = "이슈 상세 조회 성공";
    public static final String LIST_SUCCESS = "이슈 목록 조회 성공";
    public static final String UPDATE_SUCCESS = "이슈 수정 성공";
    public static final String STATUS_CHANGE_SUCCESS = "이슈 상태 변경 성공";
    public static final String DELETE_SUCCESS = "이슈 삭제 성공";
    public static final String CALENDAR_SUCCESS = "담당 이슈 캘린더 조회 성공";
    public static final String PROJECT_LIST_SUCCESS = "프로젝트 이슈 목록 조회 성공";
}
