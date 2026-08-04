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
            "스텝에 접근할 권한이 없습니다.");

    private final String code;
    private final String message;
}