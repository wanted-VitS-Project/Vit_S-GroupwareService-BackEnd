package com.group3.vitamins.project.step.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StepErrorCode implements ErrorCode {

    STEP_NAME_REQUIRED("STEP_NAME_REQUIRED",
            "스텝명을 입력해 주세요."),
    STEP_NAME_TOO_LONG("STEP_NAME_TOO_LONG",
            "스텝명은 200자를 넘을 수 없습니다."),
    STEP_DATE_RANGE_INVALID("STEP_DATE_RANGE_INVALID",
            "시작일은 종료일보다 늦을 수 없습니다."),
    STEP_NOT_FOUND("STEP_NOT_FOUND",
            "스텝을 찾을 수 없습니다."),
    STEP_ACCESS_DENIED("STEP_ACCESS_DENIED",
            "스텝에 접근할 권한이 없습니다."),
    STEP_ORDER_INVALID("STEP_ORDER_INVALID",
            "순서 목록이 비었거나 순서 값이 중복됩니다."),
    STEP_STATUS_INVALID("STEP_STATUS_INVALID",
            "허용되지 않은 상태 값입니다."),
    OPEN_ISSUE_ACTION_REQUIRED("OPEN_ISSUE_ACTION_REQUIRED",
            "미완료 이슈 처리 방식을 선택해 주세요."),
    OPEN_ISSUE_ACTION_INVALID("OPEN_ISSUE_ACTION_INVALID",
            "허용되지 않은 이슈 처리 방식입니다."),
    STEP_EDIT_DENIED("STEP_EDIT_DENIED",
            "스텝을 편집할 권한이 없습니다."),
    STEP_PERMISSION_INVALID("STEP_PERMISSION_INVALID",
            "허용되지 않은 권한 등급입니다."),
    STEP_PERMISSION_NOT_FOUND("STEP_PERMISSION_NOT_FOUND",
            "스텝 권한 오버라이드를 찾을 수 없습니다."),
    STEP_VERSION_REQUIRED("STEP_VERSION_REQUIRED",
            "버전 정보가 없습니다. 화면을 새로고침해 주세요."),
    STEP_VERSION_CONFLICT("STEP_VERSION_CONFLICT",
            "다른 사용자가 먼저 수정했습니다.");

    private final String code;
    private final String message;
}