package com.group3.vitamins.finance.infrastructure.taxinvoice.csv;

import com.group3.vitamins.finance.domain.exception.FinanceErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 확정된 컬럼 매핑으로 CSV 행을 실제 저장 형태(ParsedTaxInvoiceRow)로 변환한다. type은 행마다가 아니라
 * 업로드 요청 전체에 하나(라디오 버튼)라 여기서 다루지 않는다 — FinanceCommandService가 별도로 붙인다.
 *
 * ⚠️ 날짜 포맷은 명세에 규칙이 없어 cash_flow의 날짜 포맷 목록을 그대로 재사용해 직접 설계했다.
 */
@Component
public class TaxInvoiceCsvRowParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    public List<ParsedTaxInvoiceRow> parseRows(TaxInvoiceCsvTable table, TaxInvoiceCsvMapping mapping) {
        List<ParsedTaxInvoiceRow> result = new ArrayList<>();

        int rowNo = 0;
        for (Map<String, String> row : table.rows()) {
            rowNo++;
            // 완전히 빈 행(공백 줄 등)은 건너뛴다 — 승인번호가 없으면 저장 대상이 아니다.
            String approvalNo = valueOf(row, mapping.approvalNoColumn());
            if (approvalNo == null) {
                continue;
            }

            result.add(new ParsedTaxInvoiceRow(
                    approvalNo,
                    parseDate(requireValue(row, mapping.issuedDateColumn(), "issuedDateColumn"), rowNo),
                    valueOf(row, mapping.supplierBizNoColumn()),
                    requireValue(row, mapping.buyerBizNoColumn(), "buyerBizNoColumn"),
                    requireValue(row, mapping.buyerNameColumn(), "buyerNameColumn"),
                    parseAmount(requireValue(row, mapping.supplyAmountColumn(), "supplyAmountColumn"), rowNo),
                    parseAmount(requireValue(row, mapping.taxAmountColumn(), "taxAmountColumn"), rowNo),
                    parseAmount(requireValue(row, mapping.totalAmountColumn(), "totalAmountColumn"), rowNo),
                    valueOf(row, mapping.itemNameColumn()),
                    valueOf(row, mapping.ceoNameColumn()),
                    valueOf(row, mapping.subBizNoColumn()),
                    valueOf(row, mapping.memoColumn())
            ));
        }
        return result;
    }

    /**
     * ⚠️ 실패 메시지에 행 번호를 붙인다 (2026-08-13, CodeRabbit 지적) — 홈택스 파일은 수십~수백 행이라
     * 값만 알려주면 사용자가 어느 줄을 고쳐야 할지 찾을 수 없다. 번호는 헤더를 제외한 데이터 행 순번이다.
     */
    private LocalDate parseDate(String raw, int rowNo) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, format);
            } catch (DateTimeParseException ignored) {
                // 다음 포맷 시도
            }
        }
        throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                rowNo + "행: 작성일자 형식을 해석할 수 없습니다: " + raw);
    }

    /** 절댓값 처리 없음 — 세금계산서 금액은 방향(입출금) 개념이 없는 원본 값 그대로 저장한다. */
    private BigDecimal parseAmount(String raw, int rowNo) {
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            // PMD가 원인 예외 미보존(PreserveStackTrace)을 지적하지만 그대로 둔다 — ValidationException에
            // (코드, 메시지, 원인) 생성자가 없고 그건 global 공용 클래스라 이 PR 범위가 아니다. 게다가
            // NumberFormatException의 스택은 BigDecimal 내부라, 아래 메시지(행 번호 + 원본 값)가 진단에 더 낫다.
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                    rowNo + "행: 금액 값을 숫자로 해석할 수 없습니다: " + raw);
        }
    }

    private String valueOf(Map<String, String> row, String column) {
        return column == null ? null : row.get(column);
    }

    private String requireValue(Map<String, String> row, String column, String fieldName) {
        String value = valueOf(row, column);
        if (value == null) {
            throw new ValidationException(FinanceErrorCode.FINANCE_CSV_MAPPING_REQUIRED,
                    fieldName + " 매핑 컬럼(" + column + ")에 값이 없는 행이 있습니다.");
        }
        return value;
    }
}
