package com.group3.vitamins.project.stage.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StageErrorCode implements ErrorCode {

    STAGE_NAME_REQUIRED("STAGE_NAME_REQUIRED",
            "스테이지명을 입력해 주세요."),
    STAGE_NAME_TOO_LONG("STAGE_NAME_TOO_LONG",
            "스테이지명은 100자를 넘을 수 없습니다."),
    STAGE_NOT_FOUND("STAGE_NOT_FOUND",
            "스테이지를 찾을 수 없습니다."),
    STAGE_ORDER_INVALID("STAGE_ORDER_INVALID",
            "순서 목록이 비었거나 순서 값이 중복됩니다."),
    STAGE_MOVE_TARGET_REQUIRED("STAGE_MOVE_TARGET_REQUIRED",
            "하위 스텝을 옮길 대상을 지정해 주세요."),
    STAGE_MOVE_TARGET_INVALID("STAGE_MOVE_TARGET_INVALID",
            "하위 스텝을 옮길 수 없는 대상입니다.");

    private final String code;
    private final String message;
}