package com.group3.vitamins.vitamate.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 비타메이트 도메인 에러 코드
@Getter
@RequiredArgsConstructor
public enum VitamateErrorCode implements ErrorCode {

    VITAMATE_INVALID_REQUEST("VITAMATE_INVALID_REQUEST", "잘못된 비타메이트 요청입니다."),
    VITAMATE_ACCESS_PERMISSION_REQUIRED("VITAMATE_ACCESS_PERMISSION_REQUIRED", "비타메이트 접근 권한이 없습니다."),
    VITAMATE_WORKER_UNAUTHORIZED("VITAMATE_WORKER_UNAUTHORIZED", "내부 서비스 인증에 실패했습니다."),
    VITAMATE_ANALYSIS_NOT_FOUND("VITAMATE_ANALYSIS_NOT_FOUND", "AI 분석 이력이 존재하지 않습니다."),
    VITAMATE_BLOCK_NOT_FOUND("VITAMATE_BLOCK_NOT_FOUND", "비타메이트 블록이 존재하지 않습니다."),
    VITAMATE_FILE_VERSION_INVALID("VITAMATE_FILE_VERSION_INVALID", "분석할 수 없는 문서 버전입니다."),
    VITAMATE_IDEMPOTENCY_CONFLICT("VITAMATE_IDEMPOTENCY_CONFLICT", "같은 요청 키로 다른 분석 요청이 들어왔습니다.");

    private final String code;
    private final String message;
}
