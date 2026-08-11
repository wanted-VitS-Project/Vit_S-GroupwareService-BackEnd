package com.group3.vitamins.issue.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;

public enum IssueErrorCode implements ErrorCode {

    ISS_INVALID_REQUEST("ISS_INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    ISSUE_VERSION_CONFLICT("ISSUE_VERSION_CONFLICT", "다른 사용자가 먼저 수정했습니다."),
    ISS_STATUS_REQUIRED("ISS_STATUS_REQUIRED", "상태가 전달되지 않았습니다."),
    ISS_INVALID_STATUS("ISS_INVALID_STATUS", "지원하지 않는 상태입니다."),
    ISS_ASSIGNEE_NOT_PROJECT_MEMBER("ISS_ASSIGNEE_NOT_PROJECT_MEMBER", "담당자가 해당 Step에 접근할 수 없습니다."),
    ISS_BLOCK_STEP_MISMATCH("ISS_BLOCK_STEP_MISMATCH", "다른 Step의 Block이 포함되어 있습니다."),
    ISS_STEP_NOT_FOUND("ISS_STEP_NOT_FOUND", "Step이 존재하지 않거나 삭제되었습니다."),

    ISS_ACCESS_PERMISSION_REQUIRED("ISS_ACCESS_PERMISSION_REQUIRED", "Step 열람 권한이 없습니다."),
    ISS_EDIT_PERMISSION_REQUIRED("ISS_EDIT_PERMISSION_REQUIRED", "Step 편집 권한이 없습니다."),
    ISS_NOT_FOUND("ISS_NOT_FOUND", "Issue 없음 또는 이미 논리 삭제되었습니다."),
    ISS_ASSIGNEE_NOT_FOUND("ISS_ASSIGNEE_NOT_FOUND", "존재하지 않는 사번이 포함되어 있습니다."),
    ISS_BLOCK_NOT_FOUND("ISS_BLOCK_NOT_FOUND", "존재하지 않거나 삭제된 Block이 포함되어 있습니다.");

    private final String code;
    private final String message;

    IssueErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
