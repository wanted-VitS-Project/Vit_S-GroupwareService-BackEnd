package com.group3.vitamins.settlement.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode implements ErrorCode {

    //SETL- 쭉 작성
    FORBIDDEN("SETL-001", "편집 권한이 없습니다."),
    BLOCK_NOT_FOUND("SETL-002", "존재하지 않는 블록입니다."),
    INVALID_CONTENT("SETL-003", "내용을 입력해 주세요."),
    OUTCOME_ACCOUNT_INFO_REQUIRED("SETL-004", "출금 타입은 계좌정보가 필수입니다."),
    TYPE_REQUIRED("SETL-005", "정산 블록의 타입 지정은 필수입니다."),
    TYPE_DOWNGRADE_NOT_ALLOWED("SETL-006", "출금(OUTCOME)에서 입금(INCOME)으로는 타입을 변경할 수 없습니다."),
    ALREADY_LINKED("SETL-007", "세금계산서 또는 입출금 내역이 연결되어 있어 수정할 수 없습니다."),
    TOTAL_AMOUNT_MISMATCH("SETL-008", "같은 프로젝트의 다른 정산 블록과 총 예정 금액이 일치하지 않습니다."),
    FINANCE_ACCESS_DENIED("SETL-009", "접근 권한이 없습니다."),
    PROJECT_NOT_FOUND("SETL-010", "존재하지 않는 프로젝트입니다."),
    ROUND_NO_INVALID("SETL-011", "회차 번호는 1 이상이어야 합니다.");
    // 401(미인증)·403(RESET_REQUIRED)은 여기 도메인 코드로 안 만든다 — 전 도메인 공통으로
    // AUTH_UNAUTHENTICATED/AUTH_PASSWORD_RESET_REQUIRED 를 쓴다 (GlobalExceptionHandler·PasswordResetGateFilter).
    // 500(예상 못한 서버 오류)도 도메인 코드를 안 만든다 — GlobalExceptionHandler 의 범용 핸들러가
    // COMMON_INTERNAL_ERROR 로 받는다.

    private final String code;
    private final String message;
}
