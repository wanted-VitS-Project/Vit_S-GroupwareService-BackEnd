package com.group3.vitamins.image.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    //IMG- 쭉 작성
    UNSUPPORTED_FILE_TYPE("IMG-001", "지원하지 않는 파일 형식입니다."),
    FORBIDDEN("IMG-002", "편집 권한이 없습니다."),
    BLOCK_NOT_FOUND("IMG-003", "존재하지 않는 블록입니다."),
    CAPTION_COUNT_MISMATCH("IMG-004", "이미지 개수와 캡션 개수가 일치하지 않습니다."),
    INVALID_IMAGE_LIST("IMG-005", "요청한 이미지 목록이 유효하지 않습니다."),
    ITEM_NOT_FOUND("IMG-006", "존재하지 않는 항목입니다.");
    // 401(미인증)·403(RESET_REQUIRED)은 여기 도메인 코드로 안 만든다 — 전 도메인 공통으로
    // AUTH_UNAUTHENTICATED/AUTH_PASSWORD_RESET_REQUIRED 를 쓴다 (GlobalExceptionHandler·PasswordResetGateFilter).
    // 500(예상 못한 서버 오류)도 도메인 코드를 안 만든다 — S3 업로드 실패 등은 GlobalExceptionHandler 의
    // 범용 Exception 핸들러가 COMMON_INTERNAL_ERROR 로 받는다 (체크리스트·텍스트와 동일 컨벤션).

    private final String code;
    private final String message;
}
