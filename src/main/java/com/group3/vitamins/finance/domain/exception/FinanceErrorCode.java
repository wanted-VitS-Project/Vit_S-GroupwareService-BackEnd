package com.group3.vitamins.finance.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FinanceErrorCode implements ErrorCode {

    FINANCE_ACCESS_DENIED("FINANCE_ACCESS_DENIED", "접근 권한이 없습니다."),
    FINANCE_EDIT_ACCESS_DENIED("FINANCE_EDIT_ACCESS_DENIED", "편집 권한이 없습니다."),
    FINANCE_INVALID_CSV_FILE("FINANCE_INVALID_CSV_FILE", "유효하지 않은 형식입니다."),
    FINANCE_CSV_MAPPING_REQUIRED("FINANCE_CSV_MAPPING_REQUIRED", "필수 컬럼 매핑이 누락되었습니다."),
    FINANCE_CSV_PASSWORD_REQUIRED("FINANCE_CSV_PASSWORD_REQUIRED", "비밀번호가 필요한 파일입니다."),
    FINANCE_CSV_PASSWORD_INVALID("FINANCE_CSV_PASSWORD_INVALID", "비밀번호가 올바르지 않습니다."),
    FINANCE_CASH_FLOW_NOT_FOUND("FINANCE_CASH_FLOW_NOT_FOUND", "존재하지 않는 입출금 내역입니다."),
    FINANCE_CASH_FLOW_ALREADY_MATCHED("FINANCE_CASH_FLOW_ALREADY_MATCHED", "이미 매칭된 항목입니다."),
    FINANCE_MATCH_TARGET_NOT_FOUND("FINANCE_MATCH_TARGET_NOT_FOUND", "존재하지 않는 입출금 내역 또는 정산 블록입니다."),
    FINANCE_MATCH_TYPE_MISMATCH("FINANCE_MATCH_TYPE_MISMATCH", "입출금 구분과 정산 블록 타입이 일치하지 않습니다."),
    FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED("FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED", "이미 매칭된 정산 블록입니다."),
    FINANCE_CASH_FLOW_NOT_MATCHED("FINANCE_CASH_FLOW_NOT_MATCHED", "매칭되지 않은 항목입니다."),
    FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING("FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING", "필수 항목이 누락되었습니다."),
    FINANCE_CASH_FLOW_DUPLICATE("FINANCE_CASH_FLOW_DUPLICATE", "이미 등록된 거래입니다."),
    FINANCE_CASH_FLOW_FIELD_EDIT_NOT_ALLOWED("FINANCE_CASH_FLOW_FIELD_EDIT_NOT_ALLOWED", "메모만 수정할 수 있습니다."),
    FINANCE_CASH_FLOW_LINKED_CANNOT_DELETE("FINANCE_CASH_FLOW_LINKED_CANNOT_DELETE",
            "매칭된 항목은 삭제할 수 없습니다. 먼저 매칭을 해제해주세요."),
    FINANCE_CASH_FLOW_LINKED_CANNOT_EXCLUDE("FINANCE_CASH_FLOW_LINKED_CANNOT_EXCLUDE",
            "이미 매칭된 항목은 제외 처리할 수 없습니다."),
    FINANCE_CASH_FLOW_AMOUNT_INVALID("FINANCE_CASH_FLOW_AMOUNT_INVALID", "금액은 0보다 커야 합니다."),
    FINANCE_PAGE_QUERY_INVALID("FINANCE_PAGE_QUERY_INVALID", "페이지 조회 조건이 올바르지 않습니다."),
    FINANCE_TAX_INVOICE_NOT_FOUND("FINANCE_TAX_INVOICE_NOT_FOUND", "존재하지 않는 세금계산서입니다."),
    FINANCE_TAX_INVOICE_ALREADY_MATCHED("FINANCE_TAX_INVOICE_ALREADY_MATCHED", "이미 매칭된 항목입니다."),
    FINANCE_TAX_MATCH_TARGET_NOT_FOUND("FINANCE_TAX_MATCH_TARGET_NOT_FOUND", "존재하지 않는 세금계산서 또는 정산 블록입니다."),
    FINANCE_TAX_TYPE_MISMATCH("FINANCE_TAX_TYPE_MISMATCH", "세금계산서 구분과 정산 블록 타입이 일치하지 않습니다."),
    FINANCE_TAX_INVOICE_NOT_MATCHED("FINANCE_TAX_INVOICE_NOT_MATCHED", "매칭되지 않은 항목입니다.");
    // 401(미인증)·500(예상 못한 서버 오류)은 도메인 코드로 안 만든다 — GlobalExceptionHandler 가
    // AUTH_UNAUTHENTICATED/COMMON_INTERNAL_ERROR 로 공통 처리한다.

    private final String code;
    private final String message;
}
