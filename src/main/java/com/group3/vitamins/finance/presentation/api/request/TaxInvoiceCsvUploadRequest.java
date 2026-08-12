package com.group3.vitamins.finance.presentation.api.request;

import com.group3.vitamins.finance.application.command.TaxInvoiceCsvUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;

public record TaxInvoiceCsvUploadRequest(
        @Schema(description = "구분 (INCOME: 매출/OUTCOME: 매입), 라디오 버튼으로 선택", example = "INCOME")
        String type,
        @Schema(description = "승인번호 컬럼명", example = "승인번호")
        String approvalNoColumn,
        @Schema(description = "작성일자 컬럼명", example = "작성일자")
        String issuedDateColumn,
        @Schema(description = "공급자 사업자번호 컬럼명", example = "공급자사업자번호")
        String supplierBizNoColumn,
        @Schema(description = "공급받는자 사업자번호 컬럼명", example = "공급받는자사업자번호")
        String buyerBizNoColumn,
        @Schema(description = "공급받는자 상호 컬럼명", example = "상호")
        String buyerNameColumn,
        @Schema(description = "공급가액 컬럼명", example = "공급가액")
        String supplyAmountColumn,
        @Schema(description = "세액 컬럼명", example = "세액")
        String taxAmountColumn,
        @Schema(description = "합계금액 컬럼명", example = "합계금액")
        String totalAmountColumn,
        @Schema(description = "품목명 컬럼명 (없으면 null)", example = "품목", nullable = true)
        String itemNameColumn,
        @Schema(description = "대표자명 컬럼명 (없으면 null)", nullable = true)
        String ceoNameColumn,
        @Schema(description = "종사업장번호 컬럼명 (없으면 null)", nullable = true)
        String subBizNoColumn,
        @Schema(description = "비고/메모 컬럼명 (없으면 null)", nullable = true)
        String memoColumn,
        @Schema(description = "파일이 비밀번호로 보호돼 있으면 그 비밀번호(엑셀만 해당, CSV는 무시됨). "
                + "안 보냈는데 파일이 잠겨있으면 FINANCE_CSV_PASSWORD_REQUIRED, 틀리면 FINANCE_CSV_PASSWORD_INVALID",
                nullable = true)
        String password
) {

    public TaxInvoiceCsvUploadCommand toCommand(byte[] fileBytes, String fileName, String userId, String role) {
        return new TaxInvoiceCsvUploadCommand(
                fileBytes, fileName, password, type, approvalNoColumn, issuedDateColumn,
                supplierBizNoColumn, buyerBizNoColumn, buyerNameColumn,
                supplyAmountColumn, taxAmountColumn, totalAmountColumn,
                itemNameColumn, ceoNameColumn, subBizNoColumn, memoColumn, userId, role
        );
    }
}
