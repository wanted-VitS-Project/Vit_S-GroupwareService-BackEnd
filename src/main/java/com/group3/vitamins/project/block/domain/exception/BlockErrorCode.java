package com.group3.vitamins.project.block.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlockErrorCode implements ErrorCode {

    BLOCK_TYPE_INVALID("BLOCK_TYPE_INVALID",
            "지원하지 않는 블록 타입입니다."),
    BLOCK_TITLE_TOO_LONG("BLOCK_TITLE_TOO_LONG",
            "블록 제목은 200자를 넘을 수 없습니다."),
    BLOCK_COL_SPAN_INVALID("BLOCK_COL_SPAN_INVALID",
            "열 병합 수는 1에서 3 사이여야 합니다."),
    BLOCK_LAYOUT_INVALID("BLOCK_LAYOUT_INVALID",
            "배치 목록이 비었거나 다른 스텝의 블록이 섞여 있습니다."),
    BLOCK_UPDATE_FIELD_REQUIRED("BLOCK_UPDATE_FIELD_REQUIRED",
            "수정할 내용을 하나 이상 입력해 주세요."),
    BLOCK_NOT_FOUND("BLOCK_NOT_FOUND",
            "블록을 찾을 수 없습니다."),
    BLOCK_MOVE_TARGET_REQUIRED("BLOCK_MOVE_TARGET_REQUIRED",
            "블록을 옮길 스텝을 지정해 주세요."),
    BLOCK_MOVE_TARGET_INVALID("BLOCK_MOVE_TARGET_INVALID",
            "블록을 옮길 수 없는 스텝입니다."),
    PAYMENT_CONFIRM_BLOCK_DUPLICATED("PAYMENT_CONFIRM_BLOCK_DUPLICATED",
            "이 스텝에는 이미 정산 회차가 있습니다."),
    TAX_INVOICE_VIEW_BLOCK_DUPLICATED("TAX_INVOICE_VIEW_BLOCK_DUPLICATED",
            "이 스텝에는 이미 세금계산서 조회 블록이 있습니다."),
    ISSUE_BLOCK_STEP_MISMATCH("ISSUE_BLOCK_STEP_MISMATCH",
            "이슈와 블록이 서로 다른 스텝에 속합니다."),
    ISSUE_BLOCK_DUPLICATED("ISSUE_BLOCK_DUPLICATED",
            "이미 연결된 이슈입니다."),
    BLOCK_VERSION_REQUIRED("BLOCK_VERSION_REQUIRED",
            "버전 정보가 없습니다. 화면을 새로고침해 주세요."),
    BLOCK_VERSION_CONFLICT("BLOCK_VERSION_CONFLICT",
            "다른 사용자가 먼저 수정했습니다.");

    private final String code;
    private final String message;
}