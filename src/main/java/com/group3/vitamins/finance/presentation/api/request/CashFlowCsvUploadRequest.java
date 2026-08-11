package com.group3.vitamins.finance.presentation.api.request;

import com.group3.vitamins.finance.application.command.CashFlowCsvUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;

public record CashFlowCsvUploadRequest(
        @Schema(description = "은행명", example = "신한은행")
        String bankName,
        @Schema(description = "일시 입력 방식 (SINGLE/SEPARATE)", example = "SINGLE")
        String dateTimeMode,
        @Schema(description = "통합 일시 컬럼명 (dateTimeMode=SINGLE일 때만)", example = "날짜", nullable = true)
        String tradedDateTimeColumn,
        @Schema(description = "거래일자 컬럼명 (dateTimeMode=SEPARATE일 때만)", nullable = true)
        String tradedDateColumn,
        @Schema(description = "거래시간 컬럼명 (dateTimeMode=SEPARATE일 때만)", nullable = true)
        String tradedTimeColumn,
        @Schema(description = "금액 입력 방식 (SINGLE_WITH_TYPE/SEPARATE)", example = "SEPARATE")
        String amountMode,
        @Schema(description = "단일 금액 컬럼명 (amountMode=SINGLE_WITH_TYPE일 때만)", nullable = true)
        String amountColumn,
        @Schema(description = "구분 컬럼명 (amountMode=SINGLE_WITH_TYPE일 때만)", nullable = true)
        String typeColumn,
        @Schema(description = "입금액 컬럼명 (amountMode=SEPARATE일 때만)", example = "입금금액", nullable = true)
        String incomeAmountColumn,
        @Schema(description = "출금액 컬럼명 (amountMode=SEPARATE일 때만)", example = "출금금액", nullable = true)
        String outcomeAmountColumn,
        @Schema(description = "입금자명 컬럼명", nullable = true)
        String depositorColumn,
        @Schema(description = "적요 컬럼명", example = "적요", nullable = true)
        String memoColumn,
        @Schema(description = "잔액(거래 후 잔액) 컬럼명 — 선택. 같은 은행·시각·금액인데 실제로는 다른 거래인 "
                + "경우를 구분하는 중복 판정 보강용. 매핑 화면에 이 항목이 없으면 생략 가능(원 명세엔 없던 필드, "
                + "2026-08-10 추가)",
                example = "잔액", nullable = true)
        String balanceColumn,
        @Schema(description = "파일이 비밀번호로 보호돼 있으면 그 비밀번호(엑셀만 해당, CSV는 무시됨). "
                + "안 보냈는데 파일이 잠겨있으면 FINANCE_CSV_PASSWORD_REQUIRED, 틀리면 FINANCE_CSV_PASSWORD_INVALID",
                nullable = true)
        String password
) {

    public CashFlowCsvUploadCommand toCommand(byte[] fileBytes, String fileName, String userId, String role) {
        return new CashFlowCsvUploadCommand(
                fileBytes, fileName, password, bankName, dateTimeMode, tradedDateTimeColumn, tradedDateColumn,
                tradedTimeColumn, amountMode, amountColumn, typeColumn, incomeAmountColumn, outcomeAmountColumn,
                depositorColumn, memoColumn, balanceColumn, userId, role
        );
    }
}
