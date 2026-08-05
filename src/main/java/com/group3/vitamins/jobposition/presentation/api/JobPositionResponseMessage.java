package com.group3.vitamins.jobposition.presentation.api;

/**
 * 직급 API 성공 응답 메시지 상수 (`.ai/api/job-position.md`).
 */
public final class JobPositionResponseMessage {

    private JobPositionResponseMessage() {
    }

    public static final String LIST_SUCCESS = "직급 목록 조회 성공";
    public static final String CREATED = "직급이 생성되었습니다.";
    public static final String UPDATED = "직급이 수정되었습니다.";
    public static final String DELETED = "직급이 삭제되었습니다.";
}
