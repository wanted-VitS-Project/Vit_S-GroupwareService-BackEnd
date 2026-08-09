package com.group3.vitamins.pagepermission.presentation.api;

/** 페이지 권한 API 성공 응답 메시지 상수. */
public final class PagePermissionResponseMessage {

    public static final String MY_PAGES = "내 페이지 목록 조회 성공";
    public static final String PAGE_LIST = "페이지 목록 조회 성공";
    public static final String PAGE_ACCESS = "페이지 접근 가능자 목록 조회 성공";
    public static final String GRANTED = "페이지 권한을 부여했습니다.";
    public static final String REVOKED_STILL_ACCESSIBLE = "페이지 권한을 회수했습니다. 전역 권한으로 열람은 계속 가능합니다";
    public static final String REVOKED = "페이지 권한을 회수했습니다.";

    private PagePermissionResponseMessage() {
    }
}
